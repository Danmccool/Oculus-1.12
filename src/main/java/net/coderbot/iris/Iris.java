package net.coderbot.iris;

import com.google.common.base.Throwables;
import com.mojang.blaze3d.platform.GlDebug;
import io.github.singlerr.TextComponentExtension;
import lombok.experimental.ExtensionMethod;
import net.coderbot.iris.config.IrisConfig;
import net.coderbot.iris.gl.GLDebug;
import net.coderbot.iris.gl.shader.StandardMacros;
import net.coderbot.iris.gui.screen.ShaderPackScreen;
import net.coderbot.iris.pipeline.DeferredWorldRenderingPipeline;
import net.coderbot.iris.pipeline.FixedFunctionWorldRenderingPipeline;
import net.coderbot.iris.pipeline.PipelineManager;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.shaderpack.DimensionId;
import net.coderbot.iris.shaderpack.OptionalBoolean;
import net.coderbot.iris.shaderpack.ProgramSet;
import net.coderbot.iris.shaderpack.ShaderPack;
import net.coderbot.iris.shaderpack.discovery.ShaderpackDirectoryManager;
import net.coderbot.iris.shaderpack.materialmap.NamespacedId;
import net.coderbot.iris.shaderpack.option.OptionSet;
import net.coderbot.iris.shaderpack.option.Profile;
import net.coderbot.iris.shaderpack.option.values.MutableOptionValues;
import net.coderbot.iris.shaderpack.option.values.OptionValues;
import net.coderbot.iris.texture.pbr.PBRTextureManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipError;
import java.util.zip.ZipException;

@ExtensionMethod({TextComponentExtension.class})
@Mod(modid = Iris.MODID)
public class Iris {
    public static final String MODID = "oculus";

    /**
     * The user-facing name of the mod. Moved into a constant to facilitate
     * easy branding changes (for forks). You'll still need to change this
     * separately in mixin plugin classes & the language files.
     */
    public static final String MODNAME = "Oculus";

    public static final IrisLogging logger = new IrisLogging(MODNAME);
    private static final boolean sodiumInstalled = Loader.isModLoaded("rubidium");
    private static final Map<String, String> shaderPackOptionQueue = new HashMap<>();
    public static NamespacedId lastDimension = null;
    private static Path shaderpacksDirectory;
    private static ShaderpackDirectoryManager shaderpacksDirectoryManager;
    private static ShaderPack currentPack;
    private static String currentPackName;
    private static boolean initialized;
    private static PipelineManager pipelineManager;
    private static IrisConfig irisConfig;
    private static FileSystem zipFileSystem;
    private static KeyBinding reloadKeybind;
    private static KeyBinding toggleShadersKeybind;
    private static KeyBinding shaderpackScreenKeybind;
    // Flag variable used when reloading
    // Used in favor of queueDefaultShaderPackOptionValues() for resetting as the
    // behavior is more concrete and therefore is more likely to repair a user's issues
    private static boolean resetShaderPackOptions = false;
    private static String IRIS_VERSION;
    private static boolean fallback;

    // Wrapped in try-catch due to early initializing class
    public Iris() {
        try {
            MinecraftForge.EVENT_BUS.addListener(this::onKeyInput);

            ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        } catch (Exception ignored) {
        }
    }

    /**
     * Called once RenderSystem#initRenderer has completed. This means that we can safely access OpenGL.
     */
    public static void onRenderSystemInit() {
        if (!initialized) {
            Iris.logger.warn("Iris::onRenderSystemInit was called, but Iris::onEarlyInitialize was not called." +
                    " Trying to avoid a crash but this is an odd state.");
            return;
        }

        setDebug(irisConfig.areDebugOptionsEnabled());

        PBRTextureManager.INSTANCE.init();

        // Only load the shader pack when we can access OpenGL
        loadShaderpack();
    }

    /**
     * Called when the title screen is initialized for the first time.
     */
    public static void onLoadingComplete() {
        if (!initialized) {
            Iris.logger.warn("Iris::onLoadingComplete was called, but Iris::onEarlyInitialize was not called." +
                    " Trying to avoid a crash but this is an odd state.");
            return;
        }

        // Initialize the pipeline now so that we don't increase world loading time. Just going to guess that
        // the player is in the overworld.
        // See: https://github.com/IrisShaders/Iris/issues/323
        lastDimension = DimensionId.OVERWORLD;
        Iris.getPipelineManager().preparePipeline(DimensionId.OVERWORLD);
    }

    public static void handleKeybinds(Minecraft minecraft) {
        if (reloadKeybind.isPressed()) {
            try {
                reload();

                if (minecraft.player != null) {
                    minecraft.player.sendStatusMessage(new TextComponentTranslation("iris.shaders.reloaded"), false);
                }

            } catch (Exception e) {
                logger.error("Error while reloading Shaders for " + MODNAME + "!", e);

                if (minecraft.player != null) {
                    minecraft.player.sendStatusMessage(new TextComponentTranslation("iris.shaders.reloaded.failure", Throwables.getRootCause(e).getMessage()).withStyle(TextFormatting.RED), false);
                }
            }
        } else if (toggleShadersKeybind.isPressed()) {
            try {
                toggleShaders(minecraft, !irisConfig.areShadersEnabled());
            } catch (Exception e) {
                logger.error("Error while toggling shaders!", e);

                if (minecraft.player != null) {
                    minecraft.player.sendStatusMessage(new TextComponentTranslation("iris.shaders.toggled.failure", Throwables.getRootCause(e).getMessage()).withStyle(TextFormatting.RED), false);
                }
                setShadersDisabled();
                fallback = true;
            }
        } else if (shaderpackScreenKeybind.isPressed()) {
            minecraft.displayGuiScreen(new ShaderPackScreen(null));
        }
    }

    public static void toggleShaders(Minecraft minecraft, boolean enabled) throws IOException {
        irisConfig.setShadersEnabled(enabled);
        irisConfig.save();

        reload();
        if (minecraft.player != null) {
            minecraft.player.sendStatusMessage(enabled ? new TextComponentTranslation("iris.shaders.toggled", currentPackName) : new TextComponentTranslation("iris.shaders.disabled"), false);
        }
    }

    public static void loadShaderpack() {
        if (irisConfig == null) {
            if (!initialized) {
                throw new IllegalStateException("Iris::loadShaderpack was called, but Iris::onInitializeClient wasn't" +
                        " called yet. How did this happen?");
            } else {
                throw new NullPointerException("Iris.irisConfig was null unexpectedly");
            }
        }

        if (!irisConfig.areShadersEnabled()) {
            logger.info("Shaders are disabled because enableShaders is set to false in oculus.properties");

            setShadersDisabled();

            return;
        }

        // Attempt to load an external shaderpack if it is available
        Optional<String> externalName = irisConfig.getShaderPackName();

        if (!externalName.isPresent()) {
            logger.info("Shaders are disabled because no valid shaderpack is selected");

            setShadersDisabled();

            return;
        }

        if (!loadExternalShaderpack(externalName.get())) {
            logger.warn("Falling back to normal rendering without shaders because the shaderpack could not be loaded");
            setShadersDisabled();
            fallback = true;
        }
    }

    private static boolean loadExternalShaderpack(String name) {
        Path shaderPackRoot;
        Path shaderPackConfigTxt;

        try {
            shaderPackRoot = getShaderpacksDirectory().resolve(name);
            shaderPackConfigTxt = getShaderpacksDirectory().resolve(name + ".txt");
        } catch (InvalidPathException e) {
            logger.error("Failed to load the shaderpack \"{}\" because it contains invalid characters in its path", name);

            return false;
        }

        if (!isValidShaderpack(shaderPackRoot)) {
            logger.error("Pack \"{}\" is not valid! Can't load it.", name);
            return false;
        }

        Path shaderPackPath;

        if (shaderPackRoot.toString().endsWith(".zip")) {
            Optional<Path> optionalPath;

            try {
                optionalPath = loadExternalZipShaderpack(shaderPackRoot);
            } catch (FileSystemNotFoundException | NoSuchFileException e) {
                logger.error("Failed to load the shaderpack \"{}\" because it does not exist in your shaderpacks folder!", name);

                return false;
            } catch (ZipException e) {
                logger.error("The shaderpack \"{}\" appears to be corrupted, please try downloading it again!", name);

                return false;
            } catch (IOException e) {
                logger.error("Failed to load the shaderpack \"{}\"!", name);
                logger.error("", e);

                return false;
            }

            if (optionalPath.isPresent()) {
                shaderPackPath = optionalPath.get();
            } else {
                logger.error("Could not load the shaderpack \"{}\" because it appears to lack a \"shaders\" directory", name);
                return false;
            }
        } else {
            if (!Files.exists(shaderPackRoot)) {
                logger.error("Failed to load the shaderpack \"{}\" because it does not exist!", name);
                return false;
            }

            // If it's a folder-based shaderpack, just use the shaders subdirectory
            shaderPackPath = shaderPackRoot.resolve("shaders");
        }

        if (!Files.exists(shaderPackPath)) {
            logger.error("Could not load the shaderpack \"{}\" because it appears to lack a \"shaders\" directory", name);
            return false;
        }

        Map<String, String> changedConfigs = tryReadConfigProperties(shaderPackConfigTxt)
                .map(properties -> (Map<String, String>) (Map) properties)
                .orElse(new HashMap<>());

        changedConfigs.putAll(shaderPackOptionQueue);
        clearShaderPackOptionQueue();

        if (resetShaderPackOptions) {
            changedConfigs.clear();
        }
        resetShaderPackOptions = false;

        try {
            currentPack = new ShaderPack(shaderPackPath, changedConfigs, StandardMacros.createStandardEnvironmentDefines());

            MutableOptionValues changedConfigsValues = currentPack.getShaderPackOptions().getOptionValues().mutableCopy();

            // Store changed values from those currently in use by the shader pack
            Properties configsToSave = new Properties();
            changedConfigsValues.getBooleanValues().forEach((k, v) -> configsToSave.setProperty(k, Boolean.toString(v)));
            changedConfigsValues.getStringValues().forEach(configsToSave::setProperty);

            tryUpdateConfigPropertiesFile(shaderPackConfigTxt, configsToSave);
        } catch (Exception e) {
            logger.error("Failed to load the shaderpack \"{}\"!", name);
            logger.error("", e);

            return false;
        }

        fallback = false;
        currentPackName = name;

        logger.info("Using shaderpack: " + name);

        return true;
    }

    private static Optional<Path> loadExternalZipShaderpack(Path shaderpackPath) throws IOException {
        FileSystem zipSystem = FileSystems.newFileSystem(shaderpackPath, Iris.class.getClassLoader());
        zipFileSystem = zipSystem;

        // Should only be one root directory for a zip shaderpack
        Path root = zipSystem.getRootDirectories().iterator().next();

        Path potentialShaderDir = zipSystem.getPath("shaders");

        // If the shaders dir was immediately found return it
        // Otherwise, manually search through each directory path until it ends with "shaders"
        if (Files.exists(potentialShaderDir)) {
            return Optional.of(potentialShaderDir);
        }

        // Sometimes shaderpacks have their shaders directory within another folder in the shaderpack
        // For example Sildurs-Vibrant-Shaders.zip/shaders
        // While other packs have Trippy-Shaderpack-master.zip/Trippy-Shaderpack-master/shaders
        // This makes it hard to determine what is the actual shaders dir
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(path -> path.endsWith("shaders"))
                    .findFirst();
        }
    }

    private static void setShadersDisabled() {
        currentPack = null;
        fallback = false;
        currentPackName = "(off)";

        logger.info("Shaders are disabled");
    }

    private static void setDebug(boolean enable) {
        int success;
        if (enable) {
            success = GLDebug.setupDebugMessageCallback();
        } else {
            GLDebug
                    GlDebug.enableDebugCallback(Minecraft.getMinecraft().gameSettings.showDebugInfo, false);
            success = 1;
        }

        logger.info("Debug functionality is " + (enable ? "enabled, logging will be more verbose!" : "disabled."));
        if (Minecraft.getMinecraft().player != null) {
            Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentTranslation(success != 0 ? (enable ? "iris.shaders.debug.enabled" : "iris.shaders.debug.disabled") : "iris.shaders.debug.failure"), false);
            if (success == 2) {
                Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentTranslation("iris.shaders.debug.restart"), false);
            }
        }

        try {
            irisConfig.setDebugEnabled(enable);
            irisConfig.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Optional<Properties> tryReadConfigProperties(Path path) {
        Properties properties = new Properties();

        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                // NB: config properties are specified to be encoded with ISO-8859-1 by OptiFine,
                //     so we don't need to do the UTF-8 workaround here.
                properties.load(is);
            } catch (IOException e) {
                // TODO: Better error handling
                return Optional.empty();
            }
        }

        return Optional.of(properties);
    }

    private static void tryUpdateConfigPropertiesFile(Path path, Properties properties) {
        try {
            if (properties.isEmpty()) {
                // Delete the file or don't create it if there are no changed configs
                if (Files.exists(path)) {
                    Files.delete(path);
                }

                return;
            }

            try (OutputStream out = Files.newOutputStream(path)) {
                properties.store(out, null);
            }
        } catch (IOException e) {
            // TODO: Better error handling
        }
    }

    public static boolean isValidToShowPack(Path pack) {
        return Files.isDirectory(pack) || pack.toString().endsWith(".zip");
    }

    public static boolean isValidShaderpack(Path pack) {
        if (Files.isDirectory(pack)) {
            // Sometimes the shaderpack directory itself can be
            // identified as a shader pack due to it containing
            // folders which contain "shaders" folders, this is
            // necessary to check against that
            if (pack.equals(getShaderpacksDirectory())) {
                return false;
            }
            try (Stream<Path> stream = Files.walk(pack)) {
                return stream
                        .filter(Files::isDirectory)
                        // Prevent a pack simply named "shaders" from being
                        // identified as a valid pack
                        .filter(path -> !path.equals(pack))
                        .anyMatch(path -> path.endsWith("shaders"));
            } catch (IOException ignored) {
                // ignored, not a valid shader pack.
            }
        }

        if (pack.toString().endsWith(".zip")) {
            try (FileSystem zipSystem = FileSystems.newFileSystem(pack, Iris.class.getClassLoader())) {
                Path root = zipSystem.getRootDirectories().iterator().next();
                try (Stream<Path> stream = Files.walk(root)) {
                    return stream
                            .filter(Files::isDirectory)
                            .anyMatch(path -> path.endsWith("shaders"));
                }
            } catch (ZipError zipError) {
                // Java 8 seems to throw a ZipError instead of a subclass of IOException
                Iris.logger.warn("The ZIP at " + pack + " is corrupt");
            } catch (IOException ignored) {
                // ignored, not a valid shader pack.
            }
        }

        return false;
    }

    public static Map<String, String> getShaderPackOptionQueue() {
        return shaderPackOptionQueue;
    }

    public static void queueShaderPackOptionsFromProfile(Profile profile) {
        getShaderPackOptionQueue().putAll(profile.optionValues);
    }

    public static void queueShaderPackOptionsFromProperties(Properties properties) {
        queueDefaultShaderPackOptionValues();

        properties.stringPropertyNames().forEach(key ->
                getShaderPackOptionQueue().put(key, properties.getProperty(key)));
    }

    // Used in favor of resetShaderPackOptions as the aforementioned requires the pack to be reloaded
    public static void queueDefaultShaderPackOptionValues() {
        clearShaderPackOptionQueue();

        getCurrentPack().ifPresent(pack -> {
            OptionSet options = pack.getShaderPackOptions().getOptionSet();
            OptionValues values = pack.getShaderPackOptions().getOptionValues();

            options.getStringOptions().forEach((key, mOpt) -> {
                if (values.getStringValue(key).isPresent()) {
                    getShaderPackOptionQueue().put(key, mOpt.getOption().getDefaultValue());
                }
            });
            options.getBooleanOptions().forEach((key, mOpt) -> {
                if (values.getBooleanValue(key) != OptionalBoolean.DEFAULT) {
                    getShaderPackOptionQueue().put(key, Boolean.toString(mOpt.getOption().getDefaultValue()));
                }
            });
        });
    }

    public static void clearShaderPackOptionQueue() {
        getShaderPackOptionQueue().clear();
    }

    public static void resetShaderPackOptionsOnNextReload() {
        resetShaderPackOptions = true;
    }

    public static boolean shouldResetShaderPackOptionsOnNextReload() {
        return resetShaderPackOptions;
    }

    public static void reload() throws IOException {
        // allows shaderpacks to be changed at runtime
        irisConfig.initialize();

        // Destroy all allocated resources
        destroyEverything();

        // Load the new shaderpack
        loadShaderpack();

        // Very important - we need to re-create the pipeline straight away.
        // https://github.com/IrisShaders/Iris/issues/1330
        if (Minecraft.getMinecraft().world != null) {
            Iris.getPipelineManager().preparePipeline(Iris.getCurrentDimension());
        }
    }

    /**
     * Destroys and deallocates all created OpenGL resources. Useful as part of a reload.
     */
    private static void destroyEverything() {
        currentPack = null;

        getPipelineManager().destroyPipeline();

        // Close the zip filesystem that the shaderpack was loaded from
        //
        // This prevents a FileSystemAlreadyExistsException when reloading shaderpacks.
        if (zipFileSystem != null) {
            try {
                zipFileSystem.close();
            } catch (NoSuchFileException e) {
                logger.warn("Failed to close the shaderpack zip when reloading because it was deleted, proceeding anyways.");
            } catch (IOException e) {
                logger.error("Failed to close zip file system?", e);
            }
        }
    }

    public static NamespacedId getCurrentDimension() {
        WorldClient level = Minecraft.getMinecraft().world;

        if (level != null) {
            return new NamespacedId(level.dimension().location().getNamespace(), level.dimension().location().getPath());
        } else {
            // This prevents us from reloading the shaderpack unless we need to. Otherwise, if the player is in the
            // nether and quits the game, we might end up reloading the shaders on exit and on entry to the level
            // because the code thinks that the dimension changed.
            return lastDimension;
        }
    }

    private static WorldRenderingPipeline createPipeline(NamespacedId dimensionId) {
        if (currentPack == null) {
            // Completely disables shader-based rendering
            return new FixedFunctionWorldRenderingPipeline();
        }

        ProgramSet programs = currentPack.getProgramSet(dimensionId);

        try {
            return new DeferredWorldRenderingPipeline(programs);
        } catch (Exception e) {
            logger.error("Failed to create shader rendering pipeline, disabling shaders!", e);
            // TODO: This should be reverted if a dimension change causes shaders to compile again
            fallback = true;

            return new FixedFunctionWorldRenderingPipeline();
        }
    }

    @NotNull
    public static PipelineManager getPipelineManager() {
        if (pipelineManager == null) {
            pipelineManager = new PipelineManager(Iris::createPipeline);
        }

        return pipelineManager;
    }

    @NotNull
    public static Optional<ShaderPack> getCurrentPack() {
        return Optional.ofNullable(currentPack);
    }

    public static String getCurrentPackName() {
        return currentPackName;
    }

    public static IrisConfig getIrisConfig() {
        return irisConfig;
    }

    public static boolean isFallback() {
        return fallback;
    }

    public static String getVersion() {
        if (IRIS_VERSION == null) {
            return "Version info unknown!";
        }

        return IRIS_VERSION;
    }

    public static String getFormattedVersion() {
        ChatFormatting color;
        String version = getVersion();

        if (version.endsWith("-development-environment")) {
            color = ChatFormatting.GOLD;
            version = version.replace("-development-environment", " (Development Environment)");
        } else if (version.endsWith("-dirty") || version.contains("unknown") || version.endsWith("-nogit")) {
            color = ChatFormatting.RED;
        } else if (version.contains("+rev.")) {
            color = ChatFormatting.LIGHT_PURPLE;
        } else {
            color = ChatFormatting.GREEN;
        }

        return color + version;
    }

    public static boolean isSodiumInstalled() {
        return sodiumInstalled;
    }

    public static Path getShaderpacksDirectory() {
        if (shaderpacksDirectory == null) {
            shaderpacksDirectory = FMLPaths.GAMEDIR.get().resolve("shaderpacks");
        }

        return shaderpacksDirectory;
    }

    public static ShaderpackDirectoryManager getShaderpacksDirectoryManager() {
        if (shaderpacksDirectoryManager == null) {
            shaderpacksDirectoryManager = new ShaderpackDirectoryManager(getShaderpacksDirectory());
        }

        return shaderpacksDirectoryManager;
    }

    /**
     * Called very early on in Minecraft initialization. At this point we *cannot* safely access OpenGL, but we can do
     * some very basic setup, config loading, and environment checks.
     *
     * <p>This is roughly equivalent to Fabric Loader's ClientModInitializer#onInitializeClient entrypoint, except
     * it's entirely cross platform & we get to decide its exact semantics.</p>
     *
     * <p>This is called right before options are loaded, so we can add key bindings here.</p>
     */
    public void onEarlyInitialize() {
        try {
            if (!Files.exists(getShaderpacksDirectory())) {
                Files.createDirectories(getShaderpacksDirectory());
            }
        } catch (IOException e) {
            logger.warn("Failed to create the shaderpacks directory!");
            logger.warn("", e);
        }

        irisConfig = new IrisConfig(Minecraft.getMinecraft().gameDir.toPath().resolve("config").resolve(MODID + ".properties"));

        try {
            irisConfig.initialize();
        } catch (IOException e) {
            logger.error("Failed to initialize Oculus configuration, default values will be used instead");
            logger.error("", e);
        }

        reloadKeybind = new KeyBinding("iris.keybind.reload", Keyboard.KEY_R, "iris.keybinds");
        toggleShadersKeybind = new KeyBinding("iris.keybind.toggleShaders", Keyboard.KEY_K, "iris.keybinds");
        shaderpackScreenKeybind = new KeyBinding("iris.keybind.shaderPackSelection", Keyboard.KEY_O, "iris.keybinds");

        initialized = true;
    }

    @Mod.EventHandler
    public void onInitializeClient(final FMLPreInitializationEvent event) {
        IRIS_VERSION = Loader.instance().getModList().stream().filter(m -> m.getModId().equals(MODID)).findAny().map(ModContainer::getVersion).orElseThrow(NullPointerException::new);
        ClientRegistry.registerKeyBinding(reloadKeybind);
        ClientRegistry.registerKeyBinding(toggleShadersKeybind);
        ClientRegistry.registerKeyBinding(shaderpackScreenKeybind);
    }

    public void onKeyInput(InputEvent.KeyInputEvent event) {
        handleKeybinds(Minecraft.getMinecraft());
    }
}
