# ProGuard Configuration for HousingClient

# 1. Dictionaries
-classobfuscationdictionary dictionary.txt
-obfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt

# 2. Optimization & Shrinking
-dontshrink
-dontoptimize
-optimizationpasses 3

# 3. Obfuscation Attributes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# 4. Flatten Packages
-repackageclasses h

# 5. Warnings
-dontwarn
-ignorewarnings

# --- KEEP RULES ---

# Main Mod Class
-keep public class com.housingclient.HousingClient {
    public static void main(java.lang.String[]);
    public static com.housingclient.HousingClient instance;
    @net.minecraftforge.fml.common.Mod$EventHandler *;
}

# Event Handlers
-keepclassmembers class * {
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent *;
    @net.minecraftforge.fml.common.Mod$EventHandler *;
}



# Commands
-keep public class com.housingclient.command.commands.** {
    public <init>(...);
}

# Mixins
-keep public class com.housingclient.mixin.** {
    *;
}

# SpongePowered Mixin Library (Critical: Must be kept as it is shaded)
-keep class org.spongepowered.** {
    *;
}

# Creative Tabs (Storage)
-keep public class com.housingclient.storage.** {
    *;
}

# Modules - Keep class names and members for reflection/icon loading
-keep public class com.housingclient.module.** {
    *;
}

# Events - Keep for EventBus
-keep public class com.housingclient.event.** {
    *;
}

# Utils - Keep for potential static access/reflection
-keep public class com.housingclient.utils.** {
    *;
}

# GUI reflection safeguards
-keep public class com.housingclient.gui.** {
    *;
}

# Config/Gson
-keepclassmembers class com.housingclient.config.** {
    <fields>;
}

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
