package com.playground.fondoahorro.presentation.settlement.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

import java.time.LocalDate;

public class YearPickerController {

    @FXML
    private ComboBox<Integer> yearCombo;

    @FXML
    private void initialize() {
        int currentYear = LocalDate.now().getYear();
        var years = FXCollections.<Integer>observableArrayList();
        for (int year = currentYear; year >= currentYear - 9; year--) {
            years.add(year);
        }
        yearCombo.setItems(years);
        yearCombo.getSelectionModel().selectFirst();
    }

    public Integer getSelectedYear() {
        return yearCombo.getValue();
    }
}
