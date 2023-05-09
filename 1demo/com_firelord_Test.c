#include <pthread.h>
#include <stdio.h>
#include "com_firelord_Test.h"

pthread_t pid;
JavaVM *javaVM;
int flag;

void* java_start(void* arg) {
    JNIEnv *env = NULL;
    (*javaVM)->AttachCurrentThread(javaVM,&env,NULL);
    jclass class;
    jobject object;
    jmethodID method1;
    jmethodID method2;
    jint ret = 0;

    class = (*env)->FindClass(env, "com/firelord/Test");
    if(class == NULL) {
        printf("[jclass]error...\n");
        return;
    }
    method1 = (*env)->GetMethodID(env, class, "<init>", "()V");
    if(method1 == NULL) {
        printf("[init]error...\n");
        return;
    }
    method2 = (*env)->GetMethodID(env, class, "run", "()V");
    if(method2 == NULL) {
        printf("[run]error...\n");
        return;
    }
    object = (*env)->NewObject(env, class, method1);
    
    while(flag == 0) {
        usleep(1000000);
        printf("[native thread]INITIALIZED\n");
    }

    (*env)->CallIntMethod(env, object, method2, NULL);
}

JNIEXPORT void JNICALL Java_com_firelord_Test_start0(JNIEnv* env, jobject obj) {
    pthread_create(&pid, NULL, java_start, NULL);

    usleep(5000000);
    flag = 1;
    printf("[native thread]RUNNABLE\n");
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    javaVM = vm;
    return JNI_VERSION_1_8;
}
