package io.embrace.android.intellij.plugin.services

import io.embrace.android.intellij.plugin.EmbraceStringResources

class MyApplicationService {

    init {
        println(EmbraceStringResources.message("applicationService"))

        System.getenv("CI")
            ?: TODO("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }
}
