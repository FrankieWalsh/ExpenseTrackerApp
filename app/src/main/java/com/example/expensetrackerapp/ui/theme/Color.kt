package com.example.expensetrackerapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.example.expensetrackerapp.R
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFF95CD9E)
val PurpleGrey80 = Color(0xFFC5DAB5)
val Pink80 = Color(0xFFEA7E33)

val Purple40 = Color(0xFF2B6143)
val PurpleGrey40 = Color(0xFF758F83)
val Pink40 = Color(0xFFAB520D)

@Composable
fun Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Move `colorResource` calls here to ensure they're in a @Composable context
    val darkColorScheme = darkColorScheme(
        primary = colorResource(id = R.color.purple_80),
        secondary = colorResource(id = R.color.purple_grey_80),
        tertiary = colorResource(id = R.color.pink_80)
    )

    val lightColorScheme = lightColorScheme(
        primary = colorResource(id = R.color.purple_40),
        secondary = colorResource(id = R.color.purple_grey_40),
        tertiary = colorResource(id = R.color.pink_40)
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
