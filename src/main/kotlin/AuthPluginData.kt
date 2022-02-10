package org.laolittle.plugin

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object AuthPluginData : AutoSavePluginData("AuthPluginData") {
    val enabledGroups by value(mutableSetOf<Long>())
}

object AuthText : AutoSavePluginData("AuthText") {
    val texts by value(mutableListOf("壬戌之秋，七月既望，苏子与客泛舟游于赤壁之下。清风徐来，水波不兴。举酒属客，诵明月之诗，歌窈窕之章。少焉，月出于东山之上，徘徊于斗牛之间。白露横江，水光接天。纵一苇之所如，凌万顷之茫然。浩浩乎如冯虚御风，而不知其所止；飘飘乎如遗世独立，羽化而登仙。"))
}