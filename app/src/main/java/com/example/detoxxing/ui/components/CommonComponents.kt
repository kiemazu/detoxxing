package com.example.detoxxing.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxxing.ui.theme.InputBackground
import com.example.detoxxing.ui.theme.TextGray
import com.example.detoxxing.ui.theme.TextWhite

@Composable
fun CustomTextField(label: String, value: String, onValueChange: (String) -> Unit, isPassword: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
        TextField(
            value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = InputBackground, unfocusedContainerColor = InputBackground, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
            shape = RoundedCornerShape(10.dp), visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None, singleLine = true
        )
    }
}

@Composable
fun ClickableTextLink(normalText: String, linkText: String, onClick: () -> Unit) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = TextGray)) { append(normalText) }
            withStyle(SpanStyle(color = TextWhite, fontWeight = FontWeight.Bold)) { append(linkText) }
        }, modifier = Modifier.clickable { onClick() }, fontSize = 14.sp
    )
}