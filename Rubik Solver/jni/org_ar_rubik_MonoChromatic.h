/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_ar_rubik_MonoChromatic */

#ifndef _Included_org_ar_rubik_MonoChromatic
#define _Included_org_ar_rubik_MonoChromatic
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_ar_rubik_MonoChromatic
 * Method:    initOpenCL
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_ar_rubik_MonoChromatic_initOpenCL
  (JNIEnv *, jclass, jstring);

/*
 * Class:     org_ar_rubik_MonoChromatic
 * Method:    shutdownOpenCL
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_ar_rubik_MonoChromatic_shutdownOpenCL
  (JNIEnv *, jclass);

/*
 * Class:     org_ar_rubik_MonoChromatic
 * Method:    nativeStepOpenCL
 * Signature: (IIIIZLandroid/graphics/Bitmap;Landroid/graphics/Bitmap;)V
 */
JNIEXPORT void JNICALL Java_org_ar_rubik_MonoChromatic_nativeStepOpenCL
  (JNIEnv *, jclass, jint, jint, jint, jint, jboolean, jobject, jobject);

#ifdef __cplusplus
}
#endif
#endif
