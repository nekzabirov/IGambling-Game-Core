package infrastructure.aggregator.onegamehub.model

import domain.aggregator.adapter.BaseFreespinPreset
import domain.aggregator.adapter.PresetParam
import javax.swing.UIManager.put

class OneGameHubPreset : BaseFreespinPreset() {
    val lines = PresetParam()

    override fun toMap(): Map<String, PresetParam> {
        val hashMap = HashMap<String, PresetParam>()
        hashMap.putAll(super.toMap())
        hashMap["lines"] = lines

        return hashMap
    }

    override fun pushValue(map: Map<String, Int>) {
        super.pushValue(map)
        lines.value = map["lines"]
    }

    override fun isValid(): Boolean {
        if (lines.value == null || lines.value!! < lines.minimal!!) {
            return false
        }

        return super.isValid()
    }
}