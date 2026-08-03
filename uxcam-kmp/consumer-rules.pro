# The native Android SDK (com.uxcam:uxcam) ships its own consumer ProGuard rules for the
# classes it needs kept; the wrapper itself uses no reflection, so it needs no keeps and
# consumers' R8 can shrink it freely.
-dontwarn com.uxcam.**
