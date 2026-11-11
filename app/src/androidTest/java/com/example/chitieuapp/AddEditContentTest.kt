package com.example.chitieuapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.chitieuapp.data.local.CategoryEntity
import com.example.chitieuapp.ui.addedit.AddEditContent
import com.example.chitieuapp.ui.addedit.AddEditUiState
import com.example.chitieuapp.ui.theme.ChiTieuAppTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI test cho form thêm giao dịch (chạy trên emulator/máy thật). */
@RunWith(AndroidJUnit4::class)
class AddEditContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val food = CategoryEntity("food", "Ăn uống", false, 0xFFEF5350L, isDefault = true)
    private val transport = CategoryEntity("transport", "Di chuyển", false, 0xFF42A5F5L, isDefault = true)

    private fun initialState() = AddEditUiState(
        date = LocalDate.of(2026, 11, 9),
        categories = listOf(food, transport),
    )

    @Test
    fun saveWithEmptyAmount_showsValidationError() {
        composeRule.setContent {
            ChiTieuAppTheme {
                var state by remember { mutableStateOf(initialState()) }
                AddEditContent(
                    state = state,
                    onTypeChange = {},
                    onAmountChange = { state = state.copy(amountText = it) },
                    onCategoryChange = {},
                    onNoteChange = {},
                    onDateChange = {},
                    onTakePhoto = {},
                    onPickPhoto = {},
                    onRemovePhoto = {},
                    onSave = {
                        if (state.amountText.isBlank()) state = state.copy(amountError = "Nhập số tiền hợp lệ")
                    },
                )
            }
        }

        composeRule.onNodeWithTag("saveButton").performScrollTo().performClick()

        composeRule.onNodeWithText("Nhập số tiền hợp lệ", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun typingAmount_updatesField() {
        composeRule.setContent {
            ChiTieuAppTheme {
                var state by remember { mutableStateOf(initialState()) }
                AddEditContent(
                    state = state,
                    onTypeChange = {},
                    onAmountChange = { state = state.copy(amountText = it.filter(Char::isDigit)) },
                    onCategoryChange = {},
                    onNoteChange = {},
                    onDateChange = {},
                    onTakePhoto = {},
                    onPickPhoto = {},
                    onRemovePhoto = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithTag("amountField").performTextInput("50000")

        composeRule.onNodeWithText("50000").assertIsDisplayed()
    }

    @Test
    fun selectingCategoryAndSaving_invokesCallbacks() {
        var savedCount by mutableIntStateOf(0)
        composeRule.setContent {
            ChiTieuAppTheme {
                var state by remember { mutableStateOf(initialState()) }
                AddEditContent(
                    state = state,
                    onTypeChange = {},
                    onAmountChange = {},
                    onCategoryChange = { state = state.copy(categoryId = it) },
                    onNoteChange = {},
                    onDateChange = {},
                    onTakePhoto = {},
                    onPickPhoto = {},
                    onRemovePhoto = {},
                    onSave = { savedCount++ },
                )
            }
        }

        composeRule.onNodeWithText("Di chuyển").performClick()
        composeRule.onNodeWithText("Di chuyển").assertIsSelected()

        composeRule.onNodeWithTag("saveButton").performScrollTo().performClick()
        assertEquals(1, savedCount)
    }
}
