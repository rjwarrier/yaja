package com.mj.yaja.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.yaja.R
import com.mj.yaja.data.AppFontFamily
import com.mj.yaja.data.BackgroundTintLevel
import com.mj.yaja.data.ColorSource
import com.mj.yaja.data.CustomPalette
import com.mj.yaja.data.FabPlacement
import com.mj.yaja.data.FontScalePreference
import com.mj.yaja.data.PersonalAccentStyle
import com.mj.yaja.data.PersonalThemeSlot
import com.mj.yaja.data.ThemeColorIntensity
import com.mj.yaja.data.ThemePreference
import com.mj.yaja.ui.theme.YajaCustomPalettes
import com.mj.yaja.ui.theme.customFontFamily
import com.mj.yaja.ui.theme.customPaletteSpec
import com.mj.yaja.ui.theme.hsvColor
import com.mj.yaja.ui.theme.personalThemeSpec
import kotlin.math.roundToInt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import com.mj.yaja.ui.design.LocalAnimationPreference
import com.mj.yaja.ui.design.enterOrNone
import com.mj.yaja.ui.design.exitOrNone
import com.mj.yaja.ui.design.expressivePressMotion
import com.mj.yaja.ui.design.tweenSpec
import com.mj.yaja.ui.design.floatSpring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

private val AppearanceCardColorDark = Color(0xFF141414)
private val AppearanceCardColorAltDark = Color(0xFF171F27)
private val AppearanceCardColorLight = Color(0xFFFFFFFF)
private val AppearanceCardColorAltLight = Color(0xFFF3F6F9)

@Composable
fun AppearanceEntrySection(onNavigateToAppearance: () -> Unit) {
    SettingsDestinationEntry(
        icon = Icons.Rounded.Palette,
        title = stringResource(R.string.settings_appearance),
        subtitle = stringResource(R.string.settings_appearance_subtitle),
        onClick = onNavigateToAppearance,
    )
}

@Composable
fun AppearanceSection(
    themePreference: ThemePreference,
    onThemeSelected: (ThemePreference) -> Unit,
    colorSource: ColorSource,
    onColorSourceSelected: (ColorSource) -> Unit,
    customPalette: CustomPalette,
    onCustomPaletteSelected: (CustomPalette) -> Unit,
    themeColorIntensity: ThemeColorIntensity,
    onThemeColorIntensitySelected: (ThemeColorIntensity) -> Unit,
    backgroundTintLevel: BackgroundTintLevel,
    onBackgroundTintLevelSelected: (BackgroundTintLevel) -> Unit,
    personalThemeSlots: List<PersonalThemeSlot>,
    activePersonalThemeSlotId: Int,
    onActivePersonalThemeSlotSelected: (Int) -> Unit,
    onRenamePersonalThemeSlot: (Int, String) -> Unit,
    onPersonalThemeHueChange: (Int, Float) -> Unit,
    onPersonalThemeSaturationChange: (Int, Float) -> Unit,
    onPersonalThemeBrightnessChange: (Int, Float) -> Unit,
    onPersonalThemeAccentStyleSelected: (Int, PersonalAccentStyle) -> Unit,
    appFontFamily: AppFontFamily,
    onFontFamilySelected: (AppFontFamily) -> Unit,
    monoFontWeight: Int,
    onMonoFontWeightChange: (Int) -> Unit,
    customFontPath: String?,
    customFontName: String?,
    onPickCustomFont: () -> Unit,
    onClearCustomFont: () -> Unit,
    fabPlacement: FabPlacement,
    onFabPlacementSelected: (FabPlacement) -> Unit,
    fontScalePreference: FontScalePreference,
    onFontScaleSelected: (FontScalePreference) -> Unit,
    dataFontScalePreference: FontScalePreference,
    onDataFontScaleSelected: (FontScalePreference) -> Unit,
    followUiFontScale: Boolean,
    onFollowUiFontScaleChanged: (Boolean) -> Unit,
    selectedAppIcon: com.mj.yaja.ui.theme.AppIcon,
    onAppIconSelected: (com.mj.yaja.ui.theme.AppIcon) -> Unit
) {
    val panelColors = appearancePanelColors()
    val activePersonalThemeSlot =
        personalThemeSlots.firstOrNull { it.slotId == activePersonalThemeSlotId }
            ?: personalThemeSlots.firstOrNull()

    SectionLabel(stringResource(R.string.settings_theme))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            ThemePreference.SYSTEM to stringResource(R.string.settings_theme_system),
            ThemePreference.LIGHT to stringResource(R.string.settings_theme_light),
            ThemePreference.DARK to stringResource(R.string.settings_theme_dark),
            ThemePreference.AMOLED to stringResource(R.string.settings_theme_amoled)
        ).forEach { (theme, label) ->
            ThemeModeCard(
                modifier = Modifier.weight(1f),
                label = label,
                themePreference = theme,
                isSelected = themePreference == theme,
                panelColors = panelColors,
                onClick = { onThemeSelected(theme) }
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    SectionLabel(stringResource(R.string.settings_colors_label))
    SourceToggleRow(
        selected = colorSource,
        panelColors = panelColors,
        onSelected = onColorSourceSelected
    )

    val animationPreference = LocalAnimationPreference.current

    AnimatedVisibility(
        visible = true,
        enter = animationPreference.enterOrNone(
            expandVertically(animationSpec = animationPreference.tweenSpec(400)) + 
            fadeIn(animationSpec = animationPreference.tweenSpec(300))
        ),
        exit = animationPreference.exitOrNone(
            shrinkVertically(animationSpec = animationPreference.tweenSpec(350)) + 
            fadeOut(animationSpec = animationPreference.tweenSpec(250))
        )
    ) {
        Column {
            AnimatedVisibility(
                visible = colorSource == ColorSource.CUSTOM,
                enter = animationPreference.enterOrNone(
                    expandVertically(animationSpec = animationPreference.tweenSpec(320)) +
                        fadeIn(animationSpec = animationPreference.tweenSpec(240))
                ),
                exit = animationPreference.exitOrNone(
                    shrinkVertically(animationSpec = animationPreference.tweenSpec(260)) +
                        fadeOut(animationSpec = animationPreference.tweenSpec(180))
                )
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    PaletteGrid(
                        selected = customPalette,
                        panelColors = panelColors,
                        onSelect = onCustomPaletteSelected
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            val intensityLabels = ThemeColorIntensity.entries.map { it.label() }
            LabeledDiscreteSlider(
                title = stringResource(R.string.settings_color_intensity_title),
                selectedLabel = themeColorIntensity.label(),
                options = ThemeColorIntensity.entries,
                selectedIndex = ThemeColorIntensity.entries.indexOf(themeColorIntensity),
                panelColors = panelColors,
                onSelectIndex = { onThemeColorIntensitySelected(ThemeColorIntensity.entries[it]) },
                optionLabel = { intensityLabels[ThemeColorIntensity.entries.indexOf(it)] }
            )

            Spacer(modifier = Modifier.height(16.dp))
            val tintLabels = BackgroundTintLevel.entries.map { it.label() }
            LabeledDiscreteSlider(
                title = stringResource(R.string.settings_background_tint_title),
                selectedLabel = backgroundTintLevel.label(),
                options = BackgroundTintLevel.entries,
                selectedIndex = BackgroundTintLevel.entries.indexOf(backgroundTintLevel),
                panelColors = panelColors,
                onSelectIndex = { onBackgroundTintLevelSelected(BackgroundTintLevel.entries[it]) },
                optionLabel = { tintLabels[BackgroundTintLevel.entries.indexOf(it)] }
            )

            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(
                visible = colorSource == ColorSource.CUSTOM,
                enter = animationPreference.enterOrNone(
                    expandVertically(animationSpec = animationPreference.tweenSpec(320)) +
                        fadeIn(animationSpec = animationPreference.tweenSpec(240))
                ),
                exit = animationPreference.exitOrNone(
                    shrinkVertically(animationSpec = animationPreference.tweenSpec(260)) +
                        fadeOut(animationSpec = animationPreference.tweenSpec(180))
                )
            ) {
                Column {
                    ThemePreviewCard(
                        palette = customPalette,
                        themeColorIntensity = themeColorIntensity,
                        personalThemeSlot = activePersonalThemeSlot,
                        panelColors = panelColors
                    )
                }
            }

            AnimatedVisibility(
                visible = colorSource == ColorSource.CUSTOM && customPalette == CustomPalette.PERSONAL,
                enter = animationPreference.enterOrNone(
                    expandVertically(animationSpec = animationPreference.tweenSpec(400)) + 
                    fadeIn(animationSpec = animationPreference.tweenSpec(300))
                ),
                exit = animationPreference.exitOrNone(
                    shrinkVertically(animationSpec = animationPreference.tweenSpec(350)) + 
                    fadeOut(animationSpec = animationPreference.tweenSpec(250))
                )
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    PersonalThemesEditor(
                        slots = personalThemeSlots,
                        activeSlotId = activePersonalThemeSlotId,
                        panelColors = panelColors,
                        onSelectSlot = onActivePersonalThemeSlotSelected,
                        onRenameSlot = onRenamePersonalThemeSlot,
                        onHueChange = onPersonalThemeHueChange,
                        onSaturationChange = onPersonalThemeSaturationChange,
                        onBrightnessChange = onPersonalThemeBrightnessChange,
                        onAccentStyleSelected = onPersonalThemeAccentStyleSelected
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    SectionLabel(stringResource(R.string.settings_font))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val customFamily = remember(customFontPath) { customFontFamily(customFontPath) }
        listOf(
            AppFontFamily.SANS_SERIF to stringResource(R.string.settings_font_sans),
            AppFontFamily.SERIF to stringResource(R.string.settings_font_serif),
            AppFontFamily.MONO to stringResource(R.string.settings_font_mono),
            AppFontFamily.CUSTOM to stringResource(R.string.settings_font_custom)
        ).forEach { (font, label) ->
            FontSelectionCard(
                modifier = Modifier.weight(1f),
                label = label,
                isSelected = appFontFamily == font,
                onClick = { onFontFamilySelected(font) },
                fontFamily = font,
                customFamily = customFamily
            )
        }
    }

    AnimatedVisibility(
        visible = appFontFamily == AppFontFamily.MONO,
        enter = animationPreference.enterOrNone(
            expandVertically(animationSpec = animationPreference.tweenSpec(320)) +
                fadeIn(animationSpec = animationPreference.tweenSpec(240))
        ),
        exit = animationPreference.exitOrNone(
            shrinkVertically(animationSpec = animationPreference.tweenSpec(260)) +
                fadeOut(animationSpec = animationPreference.tweenSpec(180))
        )
    ) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            MonoWeightCard(
                monoFontWeight = monoFontWeight,
                panelColors = panelColors,
                onMonoFontWeightChange = onMonoFontWeightChange
            )
        }
    }

    AnimatedVisibility(
        visible = appFontFamily == AppFontFamily.CUSTOM,
        enter = animationPreference.enterOrNone(
            expandVertically(animationSpec = animationPreference.tweenSpec(320)) +
                fadeIn(animationSpec = animationPreference.tweenSpec(240))
        ),
        exit = animationPreference.exitOrNone(
            shrinkVertically(animationSpec = animationPreference.tweenSpec(260)) +
                fadeOut(animationSpec = animationPreference.tweenSpec(180))
        )
    ) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            CustomFontCard(
                customFontName = customFontName,
                panelColors = panelColors,
                onPickCustomFont = onPickCustomFont,
                onClearCustomFont = onClearCustomFont
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    SectionLabel(stringResource(R.string.settings_ui_font_size_label))
    FontScaleCard(
        fontScalePreference = fontScalePreference,
        panelColors = panelColors,
        onFontScaleSelected = onFontScaleSelected
    )

    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFollowUiFontScaleChanged(!followUiFontScale) }
            .padding(vertical = 8.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.settings_follow_ui_font_scale_label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = followUiFontScale,
            onCheckedChange = onFollowUiFontScaleChanged
        )
    }

    AnimatedVisibility(
        visible = !followUiFontScale,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            SectionLabel(stringResource(R.string.settings_data_font_size_label))
            FontScaleCard(
                fontScalePreference = dataFontScalePreference,
                panelColors = panelColors,
                onFontScaleSelected = onDataFontScaleSelected
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    SectionLabel(stringResource(R.string.settings_layout_label))
    Text(
        text = stringResource(R.string.settings_layout_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
    )
    FabPlacementCard(
        fabPlacement = fabPlacement,
        panelColors = panelColors,
        onFabPlacementSelected = onFabPlacementSelected
    )

    Spacer(modifier = Modifier.height(24.dp))
    SectionLabel(stringResource(R.string.settings_app_icon_label))
    Text(
        text = stringResource(R.string.settings_app_icon_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
    )
    com.mj.yaja.ui.theme.AppIcon.entries.chunked(4).forEach { rowOptions ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rowOptions.forEach { icon ->
                AppIconCard(
                    modifier = Modifier.weight(1f),
                    iconRes = icon.foregroundRes,
                    label = stringResource(icon.labelRes),
                    isSelected = selectedAppIcon == icon,
                    panelColors = panelColors,
                    onClick = { onAppIconSelected(icon) }
                )
            }
            repeat(4 - rowOptions.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }

    Spacer(modifier = Modifier.height(46.dp))
}

@Composable
private fun AppIconCard(
    modifier: Modifier = Modifier,
    iconRes: Int,
    label: String,
    isSelected: Boolean,
    panelColors: AppearancePanelColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val cardColor =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else panelColors.cardAlt

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.expressivePressMotion(interactionSource, pressedScale = 0.94f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(48.dp),
                colorFilter = if (isSelected) {
                    ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun ThemeModeCard(
    modifier: Modifier = Modifier,
    label: String,
    themePreference: ThemePreference,
    isSelected: Boolean,
    panelColors: AppearancePanelColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val selectedContainer = MaterialTheme.colorScheme.primaryContainer
    val selectedText = MaterialTheme.colorScheme.onPrimaryContainer
    val cardColor =
        if (isSelected) selectedContainer else panelColors.cardAlt
    val circleBrush =
        when (themePreference) {
            ThemePreference.SYSTEM ->
                Brush.linearGradient(listOf(Color(0xFFECECEC), Color(0xFF636363)))
            ThemePreference.LIGHT ->
                Brush.linearGradient(listOf(Color(0xFFF7F7F8), Color(0xFFEDEEF1)))
            ThemePreference.DARK ->
                Brush.linearGradient(listOf(Color(0xFF24272B), Color(0xFF1A1C1F)))
            ThemePreference.AMOLED ->
                Brush.linearGradient(listOf(Color(0xFF020202), Color(0xFF171717)))
        }

    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .height(122.dp)
            .expressivePressMotion(interactionSource, pressedScale = 0.94f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(48.dp)
                    .background(circleBrush, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint =
                            when (themePreference) {
                                ThemePreference.LIGHT,
                                ThemePreference.SYSTEM -> Color(0xFF101010)
                                ThemePreference.DARK,
                                ThemePreference.AMOLED -> Color.White
                            }
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) selectedText else panelColors.primaryText,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun SourceToggleRow(
    selected: ColorSource,
    panelColors: AppearancePanelColors,
    onSelected: (ColorSource) -> Unit
) {
    val animationPreference = LocalAnimationPreference.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = panelColors.cardAlt,
        tonalElevation = 0.dp
    ) {
        BoxWithConstraints(modifier = Modifier.padding(4.dp)) {
            val gap = 6.dp
            val controlHeight = 44.dp
            val slotWidth = (maxWidth - gap) / 2
            val targetOffset = if (selected == ColorSource.MATERIAL_YOU) 0.dp else slotWidth + gap
            val pillOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "toggle_pill_offset"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(controlHeight)
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(slotWidth)
                        .fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(999.dp)
                        )
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    SourceToggleChip(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        label = stringResource(R.string.settings_color_source_material_you),
                        selected = selected == ColorSource.MATERIAL_YOU,
                        panelColors = panelColors,
                        onClick = { onSelected(ColorSource.MATERIAL_YOU) }
                    )
                    SourceToggleChip(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        label = stringResource(R.string.settings_font_custom),
                        selected = selected == ColorSource.CUSTOM,
                        panelColors = panelColors,
                        onClick = { onSelected(ColorSource.CUSTOM) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceToggleChip(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    panelColors: AppearancePanelColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val selectedText = MaterialTheme.colorScheme.onPrimary
    val animationPreference = LocalAnimationPreference.current
    
    val textColor by animateColorAsState(
        targetValue = if (selected) selectedText else panelColors.primaryText,
        animationSpec = animationPreference.tweenSpec(250),
        label = "chip_text_color"
    )

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .expressivePressMotion(interactionSource, pressedScale = 0.94f)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = textColor
        )
    }
}

@Composable
private fun PaletteGrid(
    selected: CustomPalette,
    panelColors: AppearancePanelColors,
    onSelect: (CustomPalette) -> Unit
) {
    val rows = YajaCustomPalettes.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { spec ->
                    PaletteCard(
                        modifier = Modifier.weight(1f),
                        label = spec.label,
                        colors = spec.preview,
                        selected = spec.palette == selected,
                        panelColors = panelColors,
                        onClick = { onSelect(spec.palette) }
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PaletteCard(
    modifier: Modifier = Modifier,
    label: String,
    colors: List<Color>,
    selected: Boolean,
    panelColors: AppearancePanelColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val selectedAnchor = colors.firstOrNull() ?: MaterialTheme.colorScheme.primary
    val selectedColor = lerp(
        selectedAnchor,
        Color.Black,
        if (selectedAnchor.luminance() > 0.45f) 0.56f else 0.22f
    )
    val selectedTextColor = if (selectedColor.luminance() > 0.42f) Color(0xFF111111) else Color.White
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .height(92.dp)
            .expressivePressMotion(interactionSource, pressedScale = 0.93f),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) {
                selectedColor
            } else {
                panelColors.cardAlt
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy((-7).dp)) {
                colors.take(3).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(color, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) selectedTextColor else panelColors.primaryText
            )
        }
    }
}

@Composable
private fun <T> LabeledDiscreteSlider(
    title: String,
    selectedLabel: String,
    options: List<T>,
    selectedIndex: Int,
    panelColors: AppearancePanelColors,
    onSelectIndex: (Int) -> Unit,
    optionLabel: (T) -> String
) {
    val sliderAccent = MaterialTheme.colorScheme.primary
    val sliderInactive = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val sliderActiveTick = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
    val sliderInactiveTick = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = panelColors.cardAlt
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = panelColors.primaryText
                )
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = sliderAccent
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Slider(
                value = selectedIndex.toFloat(),
                onValueChange = { onSelectIndex(it.roundToInt().coerceIn(0, options.lastIndex)) },
                valueRange = 0f..options.lastIndex.toFloat(),
                steps = (options.size - 2).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = sliderAccent,
                    activeTrackColor = sliderAccent,
                    inactiveTrackColor = sliderInactive,
                    activeTickColor = sliderActiveTick,
                    inactiveTickColor = sliderInactiveTick
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                options.forEach { option ->
                    Text(
                        text = optionLabel(option),
                        style = MaterialTheme.typography.labelLarge,
                        color = panelColors.secondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FabPlacementCard(
    fabPlacement: FabPlacement,
    panelColors: AppearancePanelColors,
    onFabPlacementSelected: (FabPlacement) -> Unit
) {
    val options = listOf(
        FabPlacement.LEFT to stringResource(R.string.settings_fab_side_left_short),
        FabPlacement.RIGHT to stringResource(R.string.settings_fab_side_right_short)
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (placement, label) ->
            val selected = fabPlacement == placement
            val icon =
                if (placement == FabPlacement.LEFT) {
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft
                } else {
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight
                }
            SegmentedButton(
                selected = selected,
                onClick = { onFabPlacementSelected(placement) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveContainerColor = panelColors.cardAlt,
                    inactiveContentColor = panelColors.primaryText
                ),
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    }
}

@Composable
private fun ThemePreviewCard(
    palette: CustomPalette,
    themeColorIntensity: ThemeColorIntensity,
    personalThemeSlot: PersonalThemeSlot?,
    panelColors: AppearancePanelColors
) {
    val spec =
        if (palette == CustomPalette.PERSONAL && personalThemeSlot != null) {
            personalThemeSpec(personalThemeSlot)
        } else {
            customPaletteSpec(palette)
        }
    val accent = spec.primarySeed
    val secondary = spec.secondarySeed
    val tertiary = spec.tertiarySeed

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = panelColors.cardBase
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_theme_preview_title),
                style = MaterialTheme.typography.titleLarge,
                color = panelColors.primaryText
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 64.dp)
                        .background(accent, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "21",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (accent.luminance() > 0.5f) Color(0xFF151515) else Color.White
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.settings_journal_entry_preview),
                        style = MaterialTheme.typography.titleLarge,
                        color = panelColors.primaryText
                    )
                    Text(
                        text = stringResource(R.string.settings_journal_entry_preview_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = panelColors.secondaryText
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PreviewChip(stringResource(R.string.settings_preview_chip_chip), secondary)
                PreviewChip(stringResource(R.string.settings_preview_chip_accent), tertiary)
                PreviewChip(stringResource(R.string.settings_preview_chip_fab), accent, darkText = true)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_intensity_label, themeColorIntensity.label()),
                style = MaterialTheme.typography.labelLarge,
                color = panelColors.secondaryText
            )
        }
    }
}

@Composable
private fun PersonalThemesEditor(
    slots: List<PersonalThemeSlot>,
    activeSlotId: Int,
    panelColors: AppearancePanelColors,
    onSelectSlot: (Int) -> Unit,
    onRenameSlot: (Int, String) -> Unit,
    onHueChange: (Int, Float) -> Unit,
    onSaturationChange: (Int, Float) -> Unit,
    onBrightnessChange: (Int, Float) -> Unit,
    onAccentStyleSelected: (Int, PersonalAccentStyle) -> Unit
) {
    val focusedBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)
    val unfocusedBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val activeSlot = slots.firstOrNull { it.slotId == activeSlotId } ?: slots.firstOrNull() ?: return
    val spec = personalThemeSpec(activeSlot)
    val mainColor = hsvColor(activeSlot.hue, activeSlot.saturation, activeSlot.brightness)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = panelColors.cardAlt),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Text(
                text = stringResource(R.string.settings_personal_themes_title),
                style = MaterialTheme.typography.titleLarge,
                color = panelColors.primaryText
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                slots.sortedBy { it.slotId }.forEach { slot ->
                    PersonalThemeSlotCard(
                        modifier = Modifier.weight(1f),
                        slot = slot,
                        selected = slot.slotId == activeSlot.slotId,
                        panelColors = panelColors,
                        onClick = { onSelectSlot(slot.slotId) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = activeSlot.name,
                onValueChange = { onRenameSlot(activeSlot.slotId, it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_theme_name_label)) },
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = panelColors.cardBase,
                    unfocusedContainerColor = panelColors.cardBase,
                    focusedBorderColor = focusedBorder,
                    unfocusedBorderColor = unfocusedBorder,
                    focusedTextColor = panelColors.primaryText,
                    unfocusedTextColor = panelColors.primaryText,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = panelColors.secondaryText
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(mainColor, CircleShape)
                )
                Text(
                    text = stringResource(R.string.settings_main_color_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = panelColors.primaryText
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = mainColor.toHexString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = panelColors.primaryText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            PersonalColorSlider(
                label = stringResource(R.string.settings_hue_label),
                value = activeSlot.hue,
                valueRange = 0f..360f,
                panelColors = panelColors,
                onValueChange = { onHueChange(activeSlot.slotId, it) }
            )
            PersonalColorSlider(
                label = stringResource(R.string.settings_sat_label),
                value = activeSlot.saturation,
                valueRange = 0f..1f,
                panelColors = panelColors,
                onValueChange = { onSaturationChange(activeSlot.slotId, it) }
            )
            PersonalColorSlider(
                label = stringResource(R.string.settings_bright_label),
                value = activeSlot.brightness,
                valueRange = 0f..1f,
                panelColors = panelColors,
                onValueChange = { onBrightnessChange(activeSlot.slotId, it) }
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_generated_accents_title),
                style = MaterialTheme.typography.titleMedium,
                color = panelColors.primaryText
            )
            Spacer(modifier = Modifier.height(12.dp))
            val styleRows = listOf(
                listOf(PersonalAccentStyle.COMPLEMENTARY, PersonalAccentStyle.ANALOGOUS),
                listOf(PersonalAccentStyle.TRIADIC, PersonalAccentStyle.SOFT)
            )
            styleRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { style ->
                        AccentStyleCard(
                            modifier = Modifier.weight(1f),
                            style = style,
                            selected = activeSlot.accentStyle == style,
                            panelColors = panelColors,
                            onClick = { onAccentStyleSelected(activeSlot.slotId, style) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = stringResource(R.string.settings_personal_theme_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = panelColors.secondaryText
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                spec.preview.forEachIndexed { index, color ->
                    PreviewChip(
                        label = when (index) {
                            0 -> stringResource(R.string.settings_preview_chip_main)
                            1 -> stringResource(R.string.settings_preview_chip_accent)
                            else -> stringResource(R.string.settings_preview_chip_support)
                        },
                        color = color,
                        darkText = color.luminance() > 0.58f
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalThemeSlotCard(
    modifier: Modifier = Modifier,
    slot: PersonalThemeSlot,
    selected: Boolean,
    panelColors: AppearancePanelColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val selectedContainer = MaterialTheme.colorScheme.primaryContainer
    val selectedText = MaterialTheme.colorScheme.onPrimaryContainer
    val mainColor = hsvColor(slot.hue, slot.saturation, slot.brightness)
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .height(80.dp)
            .expressivePressMotion(interactionSource, pressedScale = 0.93f),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) selectedContainer else panelColors.cardBase
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(mainColor, CircleShape)
            )
            Text(
                text = slot.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) selectedText else panelColors.primaryText
            )
        }
    }
}

@Composable
private fun PersonalColorSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    panelColors: AppearancePanelColors,
    onValueChange: (Float) -> Unit
) {
    val sliderAccent = MaterialTheme.colorScheme.primary
    val sliderInactive = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val sliderActiveTick = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
    val sliderInactiveTick = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = panelColors.primaryText,
            modifier = Modifier.width(58.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = sliderAccent,
                activeTrackColor = sliderAccent,
                inactiveTrackColor = sliderInactive,
                activeTickColor = sliderActiveTick,
                inactiveTickColor = sliderInactiveTick
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AccentStyleCard(
    modifier: Modifier = Modifier,
    style: PersonalAccentStyle,
    selected: Boolean,
    panelColors: AppearancePanelColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val selectedContainer = MaterialTheme.colorScheme.primaryContainer
    val selectedText = MaterialTheme.colorScheme.onPrimaryContainer
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .height(70.dp)
            .expressivePressMotion(interactionSource, pressedScale = 0.93f),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) selectedContainer else panelColors.cardBase
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = style.label(),
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) selectedText else panelColors.primaryText
            )
        }
    }
}

@Composable
private fun PreviewChip(
    label: String,
    color: Color,
    darkText: Boolean = false
) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(999.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (darkText) Color(0xFF111111) else Color.White
        )
    }
}

// JetBrains Mono variable font exposes a single wght axis spanning 100-800.
private const val MONO_WEIGHT_SLIDER_MIN = 100f
private const val MONO_WEIGHT_SLIDER_MAX = 800f
private const val MONO_WEIGHT_SLIDER_STEP = 50

@Composable
private fun MonoWeightCard(
    monoFontWeight: Int,
    panelColors: AppearancePanelColors,
    onMonoFontWeightChange: (Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = panelColors.cardAlt
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_mono_weight_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_mono_weight_value, monoWeightLabel(monoFontWeight), monoFontWeight),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = monoFontWeight.toFloat(),
                onValueChange = { value ->
                    val snapped =
                        (value / MONO_WEIGHT_SLIDER_STEP).roundToInt() * MONO_WEIGHT_SLIDER_STEP
                    if (snapped != monoFontWeight) onMonoFontWeightChange(snapped)
                },
                valueRange = MONO_WEIGHT_SLIDER_MIN..MONO_WEIGHT_SLIDER_MAX,
                steps = ((MONO_WEIGHT_SLIDER_MAX - MONO_WEIGHT_SLIDER_MIN) / MONO_WEIGHT_SLIDER_STEP)
                    .roundToInt() - 1,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
                    inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aa",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Thin,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Aa",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CustomFontCard(
    customFontName: String?,
    panelColors: AppearancePanelColors,
    onPickCustomFont: () -> Unit,
    onClearCustomFont: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = panelColors.cardAlt
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.TextFields,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customFontName ?: stringResource(R.string.settings_no_font_selected),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (customFontName != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = stringResource(R.string.settings_custom_font_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (customFontName != null) {
                    OutlinedButton(
                        onClick = onClearCustomFont,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.action_remove)) }
                }
                FilledTonalButton(
                    onClick = onPickCustomFont,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (customFontName != null) {
                            stringResource(R.string.settings_font_replace)
                        } else {
                            stringResource(R.string.settings_choose_font_file)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun monoWeightLabel(weight: Int): String =
    when ((weight + 50) / 100 * 100) {
        100 -> stringResource(R.string.settings_mono_weight_thin)
        200 -> stringResource(R.string.settings_mono_weight_extra_light)
        300 -> stringResource(R.string.settings_mono_weight_light)
        400 -> stringResource(R.string.settings_mono_weight_regular)
        500 -> stringResource(R.string.settings_mono_weight_medium)
        600 -> stringResource(R.string.settings_mono_weight_semi_bold)
        700 -> stringResource(R.string.settings_mono_weight_bold)
        else -> stringResource(R.string.settings_mono_weight_extra_bold)
    }

@Composable
private fun FontScaleCard(
    fontScalePreference: FontScalePreference,
    panelColors: AppearancePanelColors,
    onFontScaleSelected: (FontScalePreference) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = panelColors.cardAlt
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val sliderValue = when (fontScalePreference) {
                FontScalePreference.SMALLER -> 0f
                FontScalePreference.SMALL -> 1f
                FontScalePreference.NORMAL -> 2f
                FontScalePreference.LARGE -> 3f
                FontScalePreference.LARGER -> 4f
            }

            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    val preference = when (value.roundToInt()) {
                        0 -> FontScalePreference.SMALLER
                        1 -> FontScalePreference.SMALL
                        2 -> FontScalePreference.NORMAL
                        3 -> FontScalePreference.LARGE
                        else -> FontScalePreference.LARGER
                    }
                    onFontScaleSelected(preference)
                },
                valueRange = 0f..4f,
                steps = 3,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
                    inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    FontScalePreference.SMALLER,
                    FontScalePreference.SMALL,
                    FontScalePreference.NORMAL,
                    FontScalePreference.LARGE,
                    FontScalePreference.LARGER
                ).forEach { pref ->
                    val isSelected = fontScalePreference == pref
                    Text(
                        text = "A",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (12 + (pref.scale * 8)).sp
                        ),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeColorIntensity.label(): String =
    when (this) {
        ThemeColorIntensity.MUTED -> stringResource(R.string.settings_intensity_muted)
        ThemeColorIntensity.NORMAL -> stringResource(R.string.settings_intensity_normal)
        ThemeColorIntensity.VIVID -> stringResource(R.string.settings_intensity_vivid)
        ThemeColorIntensity.POP -> stringResource(R.string.settings_intensity_pop)
    }

@Composable
private fun BackgroundTintLevel.label(): String =
    when (this) {
        BackgroundTintLevel.CLEAN -> stringResource(R.string.settings_background_tint_clean)
        BackgroundTintLevel.SOFT -> stringResource(R.string.settings_background_tint_soft)
        BackgroundTintLevel.RICH -> stringResource(R.string.settings_background_tint_rich)
        BackgroundTintLevel.DEEP -> stringResource(R.string.settings_background_tint_deep)
    }

@Composable
private fun PersonalAccentStyle.label(): String =
    when (this) {
        PersonalAccentStyle.COMPLEMENTARY -> stringResource(R.string.settings_accent_style_complementary)
        PersonalAccentStyle.ANALOGOUS -> stringResource(R.string.settings_accent_style_analogous)
        PersonalAccentStyle.TRIADIC -> stringResource(R.string.settings_accent_style_triadic)
        PersonalAccentStyle.SOFT -> stringResource(R.string.settings_accent_style_soft)
    }

private fun Color.toHexString(): String =
    String.format("#%06X", 0xFFFFFF and toArgb())

private data class AppearancePanelColors(
    val cardBase: Color,
    val cardAlt: Color,
    val primaryText: Color,
    val secondaryText: Color
)

@Composable
private fun appearancePanelColors(): AppearancePanelColors {
    val colorScheme = MaterialTheme.colorScheme
    val lightTheme = colorScheme.background.luminance() > 0.5f
    val fallbackBase = if (lightTheme) AppearanceCardColorLight else AppearanceCardColorDark
    val fallbackAlt = if (lightTheme) AppearanceCardColorAltLight else AppearanceCardColorAltDark
    val cardBase =
        if (colorScheme.surfaceContainerLow == Color.Unspecified) fallbackBase
        else colorScheme.surfaceContainerLow
    val cardAlt =
        if (colorScheme.surfaceContainerHigh == Color.Unspecified) fallbackAlt
        else colorScheme.surfaceContainerHigh
    return AppearancePanelColors(
        cardBase = cardBase,
        cardAlt = cardAlt,
        primaryText = colorScheme.onSurface,
        secondaryText = colorScheme.onSurfaceVariant
    )
}
