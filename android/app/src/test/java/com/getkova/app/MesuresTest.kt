package com.getkova.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Le format envoyé à PostHog, tel que décrit dans leur documentation de l'envoi par lots. */
class MesuresTest {
    private val date = 1_790_595_841_422L

    @Test
    fun unEvenementEstAnonymeEtSansLocalisation() {
        val evenement = Mesures.evenement("seance_validee", date, mapOf("repetitions" to 23, "record" to true, "exercice" to "POMPES"))
        assertEquals("seance_validee", evenement["event"]!!.jsonPrimitive.content)
        assertEquals("2026-09-28T11:44:01.422Z", evenement["timestamp"]!!.jsonPrimitive.content)
        val proprietes = evenement["properties"]!!.jsonObject
        assertEquals(23, proprietes["repetitions"]!!.jsonPrimitive.int)
        assertTrue(proprietes["record"]!!.jsonPrimitive.boolean)
        assertEquals("POMPES", proprietes["exercice"]!!.jsonPrimitive.content)
        assertFalse("Pas de fiche de personne.", proprietes["\$process_person_profile"]!!.jsonPrimitive.boolean)
        assertTrue("Pas de localisation par l'adresse IP.", proprietes["\$geoip_disable"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun uneProprieteDeLEvenementNePeutPasCouperLAnonymat() {
        val evenement = Mesures.evenement("x", date, mapOf("\$geoip_disable" to false, "\$process_person_profile" to true))
        val proprietes = evenement["properties"]!!.jsonObject
        assertTrue(proprietes["\$geoip_disable"]!!.jsonPrimitive.boolean)
        assertFalse(proprietes["\$process_person_profile"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun leLotPorteLaCleEtLIdentifiantSurChaqueEvenement() {
        val lignes = listOf(
            Mesures.evenement("app_ouverte", date, emptyMap()).toString(),
            "ligne abîmée",
            Mesures.evenement("ecran_vu", date, mapOf("ecran" to "ACCUEIL")).toString(),
        )
        val corps = Json.parseToJsonElement(Mesures.corps("phc_test", "identifiant-au-hasard", lignes)).jsonObject
        assertEquals("phc_test", corps["api_key"]!!.jsonPrimitive.content)
        val lot = corps["batch"]!!.jsonArray
        assertEquals("Une ligne illisible est ignorée.", 2, lot.size)
        lot.forEach { assertEquals("identifiant-au-hasard", it.jsonObject["distinct_id"]!!.jsonPrimitive.content) }
        assertEquals("ecran_vu", lot[1].jsonObject["event"]!!.jsonPrimitive.content)
    }
}
