package com.alexvt.publisher.viewui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alexvt.publisher.repositories.SettingsProfile

@Composable
fun BottomSheetView(
    isAlternativeProfile: Boolean,
    isLoading: Boolean,
    loadingMessage: String,
    hasPageLink: Boolean,
    onPageLinkClick: () -> Unit,
    settingsProfile: SettingsProfile,
    errorMessage: String,
    onCloseSettingsClick: () -> Unit,
    onPublishClick: () -> Unit,
    onSettingsProfileUpdate: (SettingsProfile) -> Unit,
    onAlternativeProfileSelection: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        // Peeking part
        Box(Modifier.fillMaxWidth().height(60.dp)) {
            // Loading bar or pull handle
            Box(Modifier.fillMaxWidth().height(10.dp)) {
                if (isLoading) {
                    LinePatternOverlayView(
                        lineColor = MaterialTheme.colors.secondary.toArgb(),
                        linePitchDp = 20.dp,
                        lineThicknessDp = 10.dp,
                        isAnimate = true,
                    )
                } else {
                    Box(
                        Modifier.align(Alignment.Center).height(5.dp).width(50.dp)
                            .alpha(0.5f).background(MaterialTheme.colors.onSurface)
                    )
                }
            }
            // Loading message
            if (isLoading) {
                Text(
                    text = loadingMessage,
                    modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 8.dp)
                )
            }
            // Error header
            if (errorMessage.isNotBlank()) {
                Text(
                    text = "Error, details below",
                    color = MaterialTheme.colors.onError,
                    fontSize = MaterialTheme.typography.subtitle1.fontSize,
                    modifier = Modifier.align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            if (!isLoading) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        Modifier.padding(horizontal = 8.dp).align(Alignment.CenterVertically)
                            .weight(1f)
                    ) {
                        SettingSwitch(
                            label = "Alternative profile",
                            value = isAlternativeProfile,
                        ) { newValue ->
                            onAlternativeProfileSelection(newValue)
                        }
                    }
                    if (hasPageLink) {
                        Button(
                            modifier = Modifier.fillMaxHeight()
                                .padding(top = 10.dp, bottom = 10.dp, end = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.primary
                            ),
                            onClick = {
                                onPageLinkClick()
                            }) {
                            Text("Copy link")
                        }
                    }
                    Button(
                        modifier = Modifier.fillMaxHeight()
                            .padding(top = 10.dp, bottom = 10.dp, end = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary
                        ),
                        onClick = {
                            onPublishClick()
                        }) {
                        Text("Publish", color = MaterialTheme.colors.onSecondary)
                    }
                }
            }
        }

        // visible when expanded

        Column(Modifier.padding(8.dp)) {
            key(isAlternativeProfile) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colors.onError,
                    fontSize = MaterialTheme.typography.subtitle1.fontSize,
                )
                Spacer(Modifier.height(8.dp))
                SettingTextField(
                    label = "GitHub Pages Repository Link",
                    hint = "https://github.com/<user>/<user>.github.io.git",
                    value = settingsProfile.githubPagesRepoUrl,
                ) { newValue ->
                    onSettingsProfileUpdate(settingsProfile.copy(githubPagesRepoUrl = newValue))
                }
                Spacer(Modifier.height(8.dp))
                SettingTextField(
                    label = "GitHub Personal Access Token",
                    hint = "Looks like ghp_aAaAa. Will be hidden as *****",
                    value = settingsProfile.githubPersonalAccessToken,
                    isValueHidden = true,
                ) { newValue ->
                    onSettingsProfileUpdate(settingsProfile.copy(githubPersonalAccessToken = newValue))
                }
                Spacer(Modifier.height(8.dp))
                SettingTextField(
                    label = "Page Header",
                    hint = "Markdown to be placed Before each New Page text",
                    value = settingsProfile.pageHeader,
                ) { newValue ->
                    onSettingsProfileUpdate(settingsProfile.copy(pageHeader = newValue))
                }
                Spacer(Modifier.height(8.dp))
                SettingTextField(
                    label = "Page Footer",
                    hint = "Markdown to be placed After each New Page text",
                    value = settingsProfile.pageFooter,
                ) { newValue ->
                    onSettingsProfileUpdate(settingsProfile.copy(pageFooter = newValue))
                }
                Spacer(Modifier.height(8.dp))
                SettingTextField(
                    label = "Header of contents in README.md",
                    hint = "## New page link will be placed under header",
                    value = settingsProfile.mainListingHeaderName,
                ) { newValue ->
                    onSettingsProfileUpdate(settingsProfile.copy(mainListingHeaderName = newValue))
                }
                Spacer(Modifier.height(8.dp))
                SettingTextField(
                    label = "Linked local files Folder Path",
                    hint = "~/Downloads/",
                    value = settingsProfile.linkedFilesFolderPath,
                ) { newValue ->
                    onSettingsProfileUpdate(settingsProfile.copy(linkedFilesFolderPath = newValue))
                }
                Spacer(Modifier.height(8.dp))
                SettingTextField(
                    label = "Name for the published changes",
                    hint = "Message to appear in Git commit history",
                    value = settingsProfile.commitMessage,
                ) { newValue ->
                    onSettingsProfileUpdate(settingsProfile.copy(commitMessage = newValue))
                }
                Spacer(Modifier.height(8.dp))
                SettingSwitch(
                    label = "Overwrite last change",
                    value = settingsProfile.overwriteLastCommit,
                ) { newValue ->
                    onSettingsProfileUpdate(settingsProfile.copy(overwriteLastCommit = newValue))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary
                    ),
                    onClick = {
                        onCloseSettingsClick()
                    }) {
                    Text("Close settings")
                }
            }
        }
    }
}

@Composable
fun SettingSwitch(
    label: String,
    value: Boolean,
    onNewValue: (Boolean) -> Unit,
) {
    var checkedState by remember { mutableStateOf(value) }
    Row(horizontalArrangement = Arrangement.Start) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterVertically).weight(1f, fill = false)
        )
        Switch(
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = MaterialTheme.colors.primary,
                checkedThumbColor = MaterialTheme.colors.secondary,
            ),
            checked = checkedState,
            onCheckedChange = { newValue ->
                checkedState = newValue
                onNewValue(newValue)
            }
        )
    }
}

@Composable
fun SettingTextField(
    label: String,
    hint: String,
    value: String,
    isValueHidden: Boolean = false,
    onNewValue: (String) -> Unit,
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = MaterialTheme.colors.secondary,
            backgroundColor = MaterialTheme.colors.secondaryVariant,
        )
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            value = textFieldValue,
            label = {
                Text(
                    text = label,
                    color = MaterialTheme.colors.onSurface,
                )
            },
            colors = TextFieldDefaults.textFieldColors(
                cursorColor = MaterialTheme.colors.onSecondary,
                focusedIndicatorColor = MaterialTheme.colors.secondary,
                backgroundColor = MaterialTheme.colors.primary,
            ),
            visualTransformation = if (isValueHidden) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            placeholder = {
                Text(text = hint)
            },
            onValueChange = { newValue ->
                textFieldValue = newValue
                onNewValue(newValue.text)
            }
        )
    }
}