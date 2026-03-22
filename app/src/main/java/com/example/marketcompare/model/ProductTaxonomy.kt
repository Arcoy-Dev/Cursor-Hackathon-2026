package com.example.marketcompare.model

/**
 * Single source of truth for allowed category/subcategory combinations.
 */
object ProductTaxonomy {
    val categories: Map<String, Set<String>> = mapOf(
        "Milchprodukte" to setOf("Milch", "Joghurt", "Käse"),
        "Obst" to setOf("Frischobst", "Beeren", "Exotisch"),
        "Gemüse" to setOf("Frischgemüse", "Hülsenfrüchte"),
        "Fleisch & Fisch" to setOf("Fleisch", "Fisch", "Wurst"),
        "Vegane Alternativen" to setOf("Pflanzliche Milch", "Fleischersatz"),
        "Getränke" to setOf("Wasser", "Säfte", "Kaffee", "Tee"),
        "Trockenwaren" to setOf("Pasta", "Reis", "Getreide"),
        "Snacks" to setOf("Chips", "Süßigkeiten", "Nüsse"),
        "Backwaren" to setOf("Brot", "Brötchen", "Gebäck"),
        "Tiefkühlprodukte" to setOf("Pizza", "Gemüse", "Eis"),
        "Konserven" to setOf("Dosen", "Gläser")
    )

    fun isValid(category: String, subcategory: String): Boolean {
        return categories[category]?.contains(subcategory) == true
    }
}
