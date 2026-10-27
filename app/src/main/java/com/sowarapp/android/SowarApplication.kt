package com.sowarapp.android

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader

/** Also tells Coil (the image library) to use our configured [ImageLoader] everywhere. */
class SowarApplication : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.imageRepository.start()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = container.imageLoader
}
