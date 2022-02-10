package org.laolittle.plugin

internal val enabledUsers = mutableSetOf<Long>()

internal val usefulRegex = Regex("[，,。./\\s]+")