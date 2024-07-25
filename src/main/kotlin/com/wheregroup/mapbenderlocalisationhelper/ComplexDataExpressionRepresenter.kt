package com.wheregroup.mapbenderlocalisationhelper

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Represent
import org.yaml.snakeyaml.representer.Representer

/**
 * Style representation to match PHPs default yaml style (in order to avoid frequent reformattings when switching between
 * using the plugin and using the mapbender:translation console commands)
 * If quotes every string that contains a reserved char.
 */
class ComplexDataExpressionRepresenter(options: DumperOptions?) : Representer(options) {
    init {
        representers[String::class.java] = RepresentQuotedString()
    }

    private inner class RepresentQuotedString : Represent {
        val reserved = arrayOf(' ', '"', '\'', '\\', ':', ';', '(', ')', '$', '%', '^', '@', ',')

        override fun representData(data: Any): Node {
            val valueToString = data.toString()
            if (valueToString.none { reserved.contains(it) }) {
                return this@ComplexDataExpressionRepresenter.representScalar(
                    Tag.STR,
                    valueToString,
                    DumperOptions.ScalarStyle.PLAIN
                )
            }
            return this@ComplexDataExpressionRepresenter.representScalar(
                Tag.STR,
                valueToString,
                DumperOptions.ScalarStyle.SINGLE_QUOTED
            )
        }
    }
}