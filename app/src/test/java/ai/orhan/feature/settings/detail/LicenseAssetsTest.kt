package ai.orhan.feature.settings.detail

import org.junit.Assert.assertEquals
import org.junit.Test

class LicenseAssetsTest {
    @Test fun sectionTitle_formatsFileName() {
        assertEquals("Lato — OFL", LicenseAssets.sectionTitle("Lato-OFL.txt"))
        assertEquals("Inter — OFL", LicenseAssets.sectionTitle("Inter-OFL.txt"))
    }
}
