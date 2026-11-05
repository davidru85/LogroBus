package com.ruizurraca.logrobus.android.app

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The app's merged manifest, as the build gives it to Robolectric: what the app declares, with every library's
 * manifest merged in. Robolectric's own parser predates Android 12, and misses some of it.
 */
internal object MergedManifest {
    const val ANDROID = "http://schemas.android.com/apk/res/android"

    /** Where the build tells Robolectric about the app, its merged manifest included. */
    private const val TEST_CONFIG = "com/android/tools/test_config.properties"

    val document: Document by lazy {
        val config =
            Properties().apply {
                MergedManifest::class.java.classLoader!!
                    .getResourceAsStream(TEST_CONFIG)!!
                    .use(::load)
            }
        DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(File(config.getProperty("android_merged_manifest")))
    }

    /** The permissions the app asks for, by name. */
    val permissions: Set<String>
        get() {
            val elements = document.getElementsByTagName("uses-permission")
            return (0 until elements.length).map { (elements.item(it) as Element).getAttributeNS(ANDROID, "name") }.toSet()
        }
}
