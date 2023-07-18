package io.embrace.android.intellij.plugin.ui.components

/**
 * This enum represents the steps of the integration process.
 *
 * - **CREATE_PROJECT**: Where the user opens the dashboard and creates or picks a project.
 * - **CONFIG_FILE_CREATION**: Where the embrace-config file is created.
 * - **DEPENDENCY_UPDATE**: Where the Gradle file is updated with Embrace dependencies.
 * - **START_METHOD_ADDITION**: Where the `Embrace.start()` method is added.
 * - **VERIFY_INTEGRATION**: The user runs the app and verifies the successful integration of the setup.
 **/
internal enum class IntegrationStep {
    CREATE_PROJECT,
    CONFIG_FILE_CREATION,
    DEPENDENCY_UPDATE,
    START_METHOD_ADDITION,
    VERIFY_INTEGRATION
}