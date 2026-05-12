package com.tk.quicksearch.tools.aiSearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Image
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors

data class LlmModelPickerOption(
    val providerId: AiSearchLlmProviderId,
    val model: GeminiTextModel,
)

@Composable
fun ModelPickerDialog(
    selectedModelId: String,
    models: List<GeminiTextModel>,
    groundingEnabled: Boolean,
    onGroundingChange: (Boolean) -> Unit,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    showGroundingToggle: Boolean = true,
    selectedProviderId: AiSearchLlmProviderId = AiSearchLlmProviderId.GEMINI,
    modelsByProvider: Map<AiSearchLlmProviderId, List<GeminiTextModel>> =
        mapOf(selectedProviderId to models),
    configuredProviderIds: Set<AiSearchLlmProviderId> = setOf(selectedProviderId),
    onProviderModelSelected: (AiSearchLlmProviderId, String) -> Unit = { _, modelId ->
        onModelSelected(modelId)
    },
) {
    val listState = rememberLazyListState()
    val selectedModel = models.firstOrNull { it.id == selectedModelId }
    val supportsGrounding = selectedModel?.supportsGrounding != false
    var searchQuery by remember { mutableStateOf("") }
    val pickerOptions =
        remember(models, modelsByProvider, configuredProviderIds, selectedProviderId) {
            val configured = configuredProviderIds.takeIf { it.isNotEmpty() } ?: setOf(selectedProviderId)
            configured
                .flatMap { providerId ->
                    val providerModels =
                        modelsByProvider[providerId]
                            ?: if (providerId == selectedProviderId) models else fallbackModels(providerId)
                    providerModels.map { model ->
                        LlmModelPickerOption(providerId = providerId, model = model)
                    }
                }
                .distinctBy { it.providerId.storageValue + ":" + it.model.id }
                .sortedWith(
                    compareBy<LlmModelPickerOption> { providerSortOrder(it.providerId) }
                        .thenBy { it.model.displayName.lowercase() },
                )
        }
    val filteredOptions =
        remember(pickerOptions, searchQuery) {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) {
                pickerOptions
            } else {
                pickerOptions.filter { option ->
                    modelSearchText(option.model).contains(query) ||
                        providerSearchName(option.providerId).lowercase().contains(query)
                }
            }
        }
    val showProviderLabels = configuredProviderIds.size > 1

    LaunchedEffect(Unit) {
        val index =
            pickerOptions.indexOfFirst {
                it.providerId == selectedProviderId && it.model.id == selectedModelId
            }
        if (index >= 0) {
            listState.scrollToItem(index)
        }
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_gemini_model_picker_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    placeholder = {
                        Text(text = stringResource(R.string.settings_model_picker_search_hint))
                    },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filteredOptions) { option ->
                        val model = option.model
                        val isSelected =
                            option.providerId == selectedProviderId && model.id == selectedModelId
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        onProviderModelSelected(option.providerId, model.id)
                                        onDismiss()
                                    }
                                    .padding(
                                        horizontal = 8.dp,
                                        vertical = 8.dp,
                                    ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onProviderModelSelected(option.providerId, model.id)
                                    onDismiss()
                                },
                                modifier = Modifier.size(16.dp).padding(end = 8.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (showProviderLabels) {
                                    Spacer(modifier = Modifier.size(4.dp))
                                    ProviderWordmark(
                                        providerId = option.providerId,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                if (showGroundingToggle && supportsGrounding) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().clickable {
                                onGroundingChange(!groundingEnabled)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = groundingEnabled,
                            onCheckedChange = onGroundingChange,
                        )
                        Text(
                            text =
                                stringResource(
                                    R.string.settings_direct_search_grounding_label,
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_close))
            }
        },
    )
}

private fun fallbackModels(providerId: AiSearchLlmProviderId): List<GeminiTextModel> =
    when (providerId) {
        AiSearchLlmProviderId.GEMINI -> GeminiModelCatalog.FALLBACK_TEXT_MODELS
        AiSearchLlmProviderId.OPENAI -> OpenAiModelCatalog.FALLBACK_TEXT_MODELS
        AiSearchLlmProviderId.ANTHROPIC -> AnthropicModelCatalog.FALLBACK_TEXT_MODELS
        AiSearchLlmProviderId.GROQ -> GroqModelCatalog.FALLBACK_TEXT_MODELS
    }

private fun providerSortOrder(providerId: AiSearchLlmProviderId): Int =
    when (providerId) {
        AiSearchLlmProviderId.GEMINI -> 0
        AiSearchLlmProviderId.OPENAI -> 1
        AiSearchLlmProviderId.ANTHROPIC -> 2
        AiSearchLlmProviderId.GROQ -> 3
    }

@Composable
private fun ProviderWordmark(
    providerId: AiSearchLlmProviderId,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    when (providerId) {
        AiSearchLlmProviderId.OPENAI -> {
            Image(
                painter = painterResource(R.drawable.openai_wordmark),
                contentDescription = stringResource(R.string.settings_ai_provider_openai),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier.size(width = 56.dp, height = 15.dp),
            )
        }
        AiSearchLlmProviderId.ANTHROPIC -> {
            Box(modifier = Modifier.size(width = 72.dp, height = 15.dp)) {
                Image(
                    painter = painterResource(R.drawable.claude_wordmark_mark),
                    contentDescription = stringResource(R.string.search_engine_claude),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Image(
                    painter = painterResource(R.drawable.claude_wordmark_type),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(contentColor),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        AiSearchLlmProviderId.GROQ -> {
            Image(
                painter = painterResource(R.drawable.groq_wordmark),
                contentDescription = stringResource(R.string.settings_ai_provider_groq),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier.size(width = 41.dp, height = 15.dp),
            )
        }
        AiSearchLlmProviderId.GEMINI -> {
            Box(modifier = Modifier.size(width = 67.dp, height = 15.dp)) {
                Image(
                    painter = painterResource(R.drawable.gemini_wordmark_mark),
                    contentDescription = stringResource(R.string.search_engine_gemini),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Image(
                    painter = painterResource(R.drawable.gemini_wordmark_type),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(contentColor),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private fun providerSearchName(providerId: AiSearchLlmProviderId): String =
    when (providerId) {
        AiSearchLlmProviderId.GEMINI -> "Gemini"
        AiSearchLlmProviderId.OPENAI -> "OpenAI"
        AiSearchLlmProviderId.ANTHROPIC -> "Claude"
        AiSearchLlmProviderId.GROQ -> "Groq"
    }

private fun modelSearchText(model: GeminiTextModel): String =
    listOf(
        model.displayName,
        model.id,
        model.id.replace('-', ' '),
        model.id.replace('_', ' '),
    )
        .joinToString(" ")
        .lowercase()
