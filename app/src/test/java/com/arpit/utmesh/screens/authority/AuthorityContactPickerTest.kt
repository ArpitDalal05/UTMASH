package com.arpit.utmesh.screens.authority

import com.arpit.utmesh.messaging.AuthorityMlsScopeType
import com.arpit.utmesh.messaging.AuthorityRosterMember
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthorityContactPickerTest {
    @Test
    fun sameAgencyMemberAlwaysOpensAgencyScopedMlsThread() {
        val entries = buildAuthorityPickerEntries(
            roster = listOf(
                AuthorityRosterMember(
                    uid = "peer",
                    name = "Peer",
                    role = "fieldteam",
                    phone = "",
                    photoUrl = "",
                    agencySlug = "afad-istanbul",
                ),
            ),
            channels = emptyList(),
        )

        assertEquals(1, entries.size)
        assertEquals(AuthorityMlsScopeType.AGENCY, entries.single().scopeType)
        assertEquals("afad-istanbul", entries.single().channelId)
        assertEquals(AuthorityPickerGroup.FIELD, entries.single().group)
    }
}
