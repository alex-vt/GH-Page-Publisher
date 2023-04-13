package com.alexvt.publisher.viewutils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun DrawerLayout(
    sideContent: @Composable () -> Unit,
    mainContent: @Composable () -> Unit,
    closeCommandFlow: Flow<Unit>,
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(Unit) {
        launch {
            closeCommandFlow.collect {
                if (drawerState.isOpen) {
                    drawerState.close()
                }
            }
        }
    }

    ModalDrawer(
        drawerState = drawerState,
        drawerContent = {
            Box(Modifier.background(MaterialTheme.colors.surface)) {
                Box(Modifier.fillMaxSize().padding(end = 20.dp)) {
                    sideContent()
                }

                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Collapse",
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(20.dp)
                        .clickable {
                            coroutineScope.launch {
                                drawerState.close()
                            }
                        }
                        .padding(2.dp)
                        .align(Alignment.TopEnd)
                )
            }
        },
    ) {
        Box {
            Box(Modifier.fillMaxSize().padding(start = 8.dp)) {
                mainContent()
            }

            Box(
                Modifier.width(8.dp).wrapContentHeight(),
            ) {
                Icon(
                    Icons.Default.List,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .requiredWidth(32.dp)
                        .requiredHeight(32.dp)
                        .clickable {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        }
                        .padding(
                            top = 8.dp,
                            bottom = 8.dp,
                            end = 13.dp
                        ) // icon is 16 by 16, clipped
                )
            }
        }
    }
}
