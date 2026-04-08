package iad1tya.echo.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.R
import iad1tya.echo.music.listentogether.ConnectionState
import iad1tya.echo.music.listentogether.RoomRole
import iad1tya.echo.music.constants.ListenTogetherAutoApprovalKey
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.utils.rememberPreference
import iad1tya.echo.music.viewmodels.ListenTogetherViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ListenTogetherScreen(
    navController: NavController,
    viewModel: ListenTogetherViewModel = hiltViewModel(),
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val roomState by viewModel.roomState.collectAsState()
    val role by viewModel.role.collectAsState()
    val (autoApproveNewUsers, onAutoApproveNewUsersChange) = rememberPreference(
        key = ListenTogetherAutoApprovalKey,
        defaultValue = false,
    )

    var username by rememberSaveable { mutableStateOf("") }
    var roomCode by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Listen Together",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Create a room, invite friends, and keep playback in sync.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        )
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "Status: ${connectionStateLabel(connectionState)}",
                                    fontWeight = FontWeight.Medium,
                                )
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                when (connectionState) {
                                    ConnectionState.DISCONNECTED, ConnectionState.ERROR -> Color(0xFFC62828)
                                    ConnectionState.CONNECTED -> Color(0xFF2E7D32)
                                    else -> MaterialTheme.colorScheme.outline
                                },
                            ),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = when (connectionState) {
                                    ConnectionState.DISCONNECTED, ConnectionState.ERROR -> Color(0xFFFFD6D6)
                                    ConnectionState.CONNECTED -> Color(0xFFDFF4E2)
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                labelColor = when (connectionState) {
                                    ConnectionState.DISCONNECTED, ConnectionState.ERROR -> Color(0xFFB71C1C)
                                    ConnectionState.CONNECTED -> Color(0xFF1B5E20)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            ),
                        )
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Session Snapshot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            InfoPill(
                                modifier = Modifier.weight(1f),
                                title = "Room",
                                value = roomState?.roomCode ?: "Not joined",
                            )
                            InfoPill(
                                modifier = Modifier.weight(1f),
                                title = "Role",
                                value = roleLabel(role.name),
                            )
                        }

                        if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) {
                            OutlinedButton(onClick = { viewModel.connect() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Connect to server")
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.disconnect() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Disconnect")
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = "Auto-approve new users",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = if (role == RoomRole.HOST) {
                                        "Automatically approve join requests while you are host"
                                    } else {
                                        "Applied when you host a room"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = autoApproveNewUsers,
                                onCheckedChange = onAutoApproveNewUsersChange,
                            )
                        }
                    }
                }
            }

            if (roomState == null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Identity & Room", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Use your display name and a valid room code to start syncing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username") },
                                placeholder = { Text("e.g. Aditya") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = roomCode,
                                onValueChange = { roomCode = it.uppercase() },
                                label = { Text("Room code") },
                                placeholder = { Text("ABCD") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            FilledTonalButton(
                                onClick = { viewModel.createRoom(username.trim()) },
                                enabled = username.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Create room")
                            }
                            Button(
                                onClick = { viewModel.joinRoom(roomCode.trim(), username.trim()) },
                                enabled = username.isNotBlank() && roomCode.length >= 4,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Join room")
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "You are live in ${roomState?.roomCode}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = "Leaving the room will stop sync with everyone connected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                            )
                            Button(onClick = { viewModel.leaveRoom() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Leave room")
                            }
                        }
                    }
                }

                item {
                    Text("Participants (${roomState?.users?.size ?: 0})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(roomState?.users ?: emptyList(), key = { it.userId }) { user ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 9.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    text = user.username.take(1).uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                            Spacer(modifier = Modifier.padding(horizontal = 5.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.username, fontWeight = FontWeight.Medium)
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = {
                                        Text(
                                            if (user.isHost) "Host" else "Guest",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (user.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(64.dp)) }
        }
    }
}

@Composable
private fun InfoPill(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun connectionStateLabel(state: ConnectionState): String =
    when (state) {
        ConnectionState.DISCONNECTED -> "Offline"
        ConnectionState.CONNECTING -> "Connecting"
        ConnectionState.RECONNECTING -> "Reconnecting"
        ConnectionState.CONNECTED -> "Online"
        ConnectionState.ERROR -> "Connection error"
    }

private fun roleLabel(role: String): String =
    when (role.uppercase()) {
        "HOST" -> "Host"
        "GUEST" -> "Guest"
        else -> "Not in room"
    }
