package com.ruizurraca.logrobus.android.feature.nearme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.ruizurraca.logrobus.android.core.designsystem.LineBadge
import com.ruizurraca.logrobus.android.core.designsystem.Tokens
import com.ruizurraca.logrobus.feature.nearme.domain.StopSearchResultUi
import com.ruizurraca.logrobus.android.core.designsystem.R as DS

/**
 * LB-209 (FR-NEAR-05): the search field. It is a plain text field rather than the app bar's, so it stays in the list
 * where the rider already is, and every keystroke asks the ViewModel once.
 */
@Composable
internal fun StopSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Tokens.Spacing.l, vertical = Tokens.Spacing.s),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.nearme_search_prompt)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        painterResource(DS.drawable.ic_clear),
                        contentDescription = stringResource(R.string.nearme_search_clear),
                    )
                }
            }
        },
    )
}

/**
 * LB-209 (FR-STOP-01, DD-16): one result. Its name over the badge and headsign of the line that reaches it, which is
 * how two stops with one name are told apart, and one utterance so TalkBack reads it whole.
 */
@Composable
internal fun StopSearchRow(
    result: StopSearchResultUi,
    onOpenStop: (Int) -> Unit,
) {
    val line = result.lines.firstOrNull()
    val headsign = line?.headsign?.let { stringResource(R.string.nearme_search_towards, it) }
    val spoken = listOfNotNull(result.stop.name, line?.code, headsign).joinToString(", ")
    ListItem(
        headlineContent = { Text(result.stop.name, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            if (line != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LineBadge(line.code, line.color, line.onColor)
                    headsign?.let { Text(it, Modifier.padding(start = Tokens.Spacing.s)) }
                }
            }
        },
        modifier =
            Modifier
                .clickable(onClickLabel = stringResource(R.string.nearme_nearby_open_stop), role = Role.Button) {
                    onOpenStop(result.stop.id)
                }.clearAndSetSemantics { contentDescription = spoken },
    )
}

/** FR-NEAR-05: a query that found nothing says so rather than showing an empty list. */
@Composable
internal fun SearchNoMatches() {
    ListItem(headlineContent = { Text(stringResource(R.string.nearme_search_no_matches)) })
}
