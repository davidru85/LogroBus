package com.ruizurraca.logrobus.android.app

import android.content.pm.ApplicationInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RuntimeEnvironment
import org.xmlpull.v1.XmlPullParser

/** NFR-SEC-01 and LB-005: the app never sends HTTP in cleartext. */
@RunWith(AndroidJUnit4::class)
class NetworkSecurityTest {
    // Robolectric starts a new Application, and so a new Koin graph, for every test in one process (ADR-0014).
    @After
    fun stopTheGraph() = stopKoin()

    private val application = RuntimeEnvironment.getApplication()

    @Test
    fun lb005_androidRefusesCleartextTraffic() {
        // The manifest's android:networkSecurityConfig, a field the SDK hides from apps but Robolectric fills in.
        val info = application.packageManager.getApplicationInfo(application.packageName, 0)
        val config = ApplicationInfo::class.java.getField("networkSecurityConfigRes").getInt(info)
        assertEquals(R.xml.network_security_config, config)

        // One base-config that denies cleartext, and no domain-config that could allow it back.
        assertEquals(
            listOf("network-security-config" to null, "base-config" to "false"),
            elements(R.xml.network_security_config),
        )
    }

    /** Each element of an XML resource, with its cleartextTrafficPermitted attribute. */
    private fun elements(xml: Int): List<Pair<String, String?>> {
        val parser = application.resources.getXml(xml)
        val elements = mutableListOf<Pair<String, String?>>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                // The network security config's attributes have no namespace.
                elements += parser.name to parser.getAttributeValue(null, "cleartextTrafficPermitted")
            }
        }
        return elements
    }
}
