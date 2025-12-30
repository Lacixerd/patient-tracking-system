package org.example.patientmanagementsystem;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

// PDF Kütüphanesi
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class HelloController {

    @FXML private TextField txtSearch;
    @FXML private Button btnSearch, btnMenu, btnAddPatient, btnRemovePatient, btnPrescription, btnExportPdf, btnEditDiseases;
    @FXML private VBox sidebarPane;

    @FXML private TableView<Patient> tablePatients;
    @FXML private TableColumn<Patient, String> colTC, colName, colSurname, colGender, colBlood, colStatus;
    @FXML private TableColumn<Patient, Integer> colAge;

    @FXML private Label lblSelectedPatientName, lblCriticalWarning;
    @FXML private TableView<HealthRecord> tableHistory;
    @FXML private GridPane dynamicInputContainer;
    @FXML private TextArea txtNotes;
    @FXML private Button btnAddEntry;

    private ObservableList<Patient> patientList = FXCollections.observableArrayList();
    private List<DiseaseTemplate> diseaseTemplates = new ArrayList<>();
    private Patient selectedPatient;
    private Map<String, Control> dynamicInputs = new HashMap<>();
    private DatePicker dynamicDatePicker;
    private boolean isMenuOpen = true;

    @FXML
    public void initialize() {
        loadData();

        // --- TABLO AYARLARI ---
        colTC.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTcNo()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colSurname.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSurname()));
        colGender.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGender()));
        colAge.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getAge()));
        colBlood.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBloodGroup()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDiseaseName()));

        colAge.setStyle("-fx-alignment: CENTER;");
        colGender.setStyle("-fx-alignment: CENTER;");
        colBlood.setStyle("-fx-alignment: CENTER;");

        // --- FİLTRELEME ---
        FilteredList<Patient> filteredData = new FilteredList<>(patientList, p -> true);
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(patient -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lower = newValue.toLowerCase();
                return patient.getName().toLowerCase().contains(lower) ||
                        patient.getSurname().toLowerCase().contains(lower) ||
                        patient.getTcNo().contains(lower);
            });
        });
        SortedList<Patient> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablePatients.comparatorProperty());
        tablePatients.setItems(sortedData);

        // --- SAĞ TIK MENÜLERİ ---
        setupTableContextMenus();

        // --- BUTON FONKSİYONLARI ---
        btnMenu.setOnAction(e -> toggleMenu());
        tablePatients.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) loadPatientDetails(newVal);
        });
        btnAddPatient.setOnAction(e -> showAddPatientDialog());
        btnRemovePatient.setOnAction(e -> removeSelectedPatient());
        btnAddEntry.setOnAction(e -> saveHealthRecord());

        if (btnPrescription != null) btnPrescription.setOnAction(e -> showAddPrescriptionDialog());
        if (btnExportPdf != null) btnExportPdf.setOnAction(e -> exportToPdf());
        if (btnEditDiseases != null) btnEditDiseases.setOnAction(e -> showDiseaseListDialog());
    }

    // --- Kritik Değer Kontrolü ---
    private void checkCriticalValues(Patient patient, Map<String, String> inputValues) {
        Map<String, String> thresholds = patient.getDisease().getCriticalThresholds();
        StringBuilder warnings = new StringBuilder();

        for (Map.Entry<String, String> entry : inputValues.entrySet()) {
            String param = entry.getKey();
            String valueStr = entry.getValue();
            String condition = thresholds.get(param);

            if (condition != null && !valueStr.isEmpty()) {
                try {
                    double value = Double.parseDouble(valueStr.replaceAll("[^0-9.,]", "").replace(",", "."));
                    boolean isCritical = false;

                    if (condition.startsWith(">")) {
                        double limit = Double.parseDouble(condition.substring(1));
                        if (value > limit) isCritical = true;
                    } else if (condition.startsWith("<")) {
                        double limit = Double.parseDouble(condition.substring(1));
                        if (value < limit) isCritical = true;
                    }

                    if (isCritical) {
                        warnings.append("• ").append(param).append(" değeri (").append(valueStr)
                                .append(") kritik seviyede! (Sınır: ").append(condition).append(")\n");
                    }
                } catch (NumberFormatException e) { }
            }
        }

        if (warnings.length() > 0) {
            showNotificationBubble(warnings.toString());
        }
    }

    // --- Bildirim Baloncuğu ---
    private void showNotificationBubble(String message) {
        Popup popup = new Popup();

        VBox content = new VBox(10);
        content.setStyle("-fx-background-color: #ff7675; -fx-padding: 15; -fx-background-radius: 10; -fx-border-color: #d63031; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
        content.setPrefWidth(320);

        Label lblTitle = new Label("⚠️ KRİTİK UYARI");
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");

        Label lblMsg = new Label(message);
        lblMsg.setWrapText(true);
        lblMsg.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

        Button btnClose = new Button("Tamam, Farkındayım");
        btnClose.setStyle("-fx-background-color: white; -fx-text-fill: #d63031; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        btnClose.setMaxWidth(Double.MAX_VALUE);
        btnClose.setOnAction(e -> popup.hide());

        content.getChildren().addAll(lblTitle, lblMsg, btnClose);
        popup.getContent().add(content);

        Stage stage = (Stage) tablePatients.getScene().getWindow();
        popup.show(stage, stage.getX() + stage.getWidth() - 340, stage.getY() + stage.getHeight() - 200);

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(7), e -> popup.hide()));
        timeline.play();
    }

    private void showDiseaseListDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Hastalık Yönetimi");
        dialog.setHeaderText("Düzenlemek istediğiniz hastalığı seçiniz:");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ListView<DiseaseTemplate> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(diseaseTemplates));

        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                DiseaseTemplate selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showEditSpecificDiseaseDialog(selected);
                    listView.refresh();
                }
            }
        });

        Button btnEditSelected = new Button("Seçileni Düzenle");
        btnEditSelected.setMaxWidth(Double.MAX_VALUE);
        btnEditSelected.setOnAction(e -> {
            DiseaseTemplate selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditSpecificDiseaseDialog(selected);
                listView.refresh();
            } else {
                showAlert("Uyarı", "Lütfen bir hastalık seçiniz.");
            }
        });

        VBox content = new VBox(10, listView, btnEditSelected);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void showEditSpecificDiseaseDialog(DiseaseTemplate disease) {
        // 1. Dialog Kurulumu
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Hastalık Düzenle");
        dialog.setHeaderText(disease.getName() + " verilerini düzenle");

        ButtonType saveBtn = new ButtonType("Kaydet", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // 2. Ana Layout (VBox kullanarak dikey dizilim)
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setPrefWidth(450);

        // --- A. Hastalık Adı ---
        VBox nameBox = new VBox(5);
        Label lblName = new Label("Hastalık Adı:");
        lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #d35400;");
        TextField txtName = new TextField(disease.getName());
        nameBox.getChildren().addAll(lblName, txtName);

        // --- B. Dinamik Parametre ve Eşik Alanı ---
        Label lblParams = new Label("Parametreler ve Kritik Eşikler:");
        lblParams.setStyle("-fx-font-weight: bold; -fx-text-fill: #d35400;");

        // Parametrelerin listeleneceği kapsayıcı
        VBox paramsContainer = new VBox(10);

        // ScrollPane: Parametre çok olursa kaydırma çubuğu çıksın
        ScrollPane scrollPane = new ScrollPane(paramsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Mevcut verileri yükleme fonksiyonu (Interface içinde tanımlayamadığımız için Runnable/Consumer mantığı)
        // Her bir satır şunları içerir: [Parametre Adı] - [Eşik Değeri] - [Sil Butonu]
        for (String param : disease.getParameters()) {
            HBox row = createParamRow(param, disease.getThreshold(param), paramsContainer);
            paramsContainer.getChildren().add(row);
        }

        // "+ Parametre Ekle" Butonu
        Button btnAddParam = new Button("+ Parametre & Eşik Ekle");
        btnAddParam.setMaxWidth(Double.MAX_VALUE);
        btnAddParam.setOnAction(e -> {
            // Boş bir satır ekle
            paramsContainer.getChildren().add(createParamRow("", "", paramsContainer));
        });

        VBox paramSection = new VBox(5, lblParams, scrollPane, btnAddParam);

        // --- C. İlaçlar ---
        VBox medsBox = new VBox(5);
        Label lblMeds = new Label("İlaçlar:");
        lblMeds.setStyle("-fx-font-weight: bold; -fx-text-fill: #d35400;");

        // Mevcut ilaçları Text Area'ya dök
        String medsStr = String.join("\n", disease.getDefaultMedicines());
        TextArea txtMeds = new TextArea(medsStr);
        txtMeds.setPromptText("Her satıra bir ilaç gelecek şekilde...");
        txtMeds.setPrefHeight(80);
        medsBox.getChildren().addAll(lblMeds, txtMeds);

        // Tüm parçaları birleştir
        mainLayout.getChildren().addAll(nameBox, paramSection, medsBox);
        dialog.getDialogPane().setContent(mainLayout);

        // --- 3. Kaydetme ---
        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                // a. İsim Güncelle
                disease.setName(txtName.getText().trim());

                // b. Parametreleri ve Eşikleri Topla
                List<String> newParams = new ArrayList<>();
                Map<String, String> newThresholds = new HashMap<>();

                // Container içindeki her HBox (satır) için döngü
                for (javafx.scene.Node node : paramsContainer.getChildren()) {
                    if (node instanceof HBox) {
                        HBox row = (HBox) node;
                        // HBox içindeki elemanları sırayla al: 0->Ad, 1->Eşik
                        TextField tName = (TextField) row.getChildren().get(0);
                        TextField tThres = (TextField) row.getChildren().get(1);

                        String pName = tName.getText().trim();
                        String pThres = tThres.getText().trim();

                        if (!pName.isEmpty()) {
                            newParams.add(pName); // Parametre listesine ekle
                            if (!pThres.isEmpty()) {
                                newThresholds.put(pName, pThres); // Eşik varsa Map'e ekle
                            }
                        }
                    }
                }
                disease.setParameters(newParams);
                disease.setCriticalThresholds(newThresholds);

                // c. İlaçları Güncelle
                List<String> newMeds = Arrays.stream(txtMeds.getText().split("\n"))
                        .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                disease.setDefaultMedicines(newMeds);

                return true;
            }
            return null;
        });

        Optional<Boolean> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            if (updated) {
                saveData(); // CSV'ye yaz
                tablePatients.refresh(); // Tabloyu yenile
                if (selectedPatient != null && selectedPatient.getDisease().equals(disease)) {
                    loadPatientDetails(selectedPatient); // Detayları yenile
                }
                showAlert("Başarılı", "Hastalık bilgileri güncellendi.");
            }
        });
    }

    // YARDIMCI METOT: Dinamik Satır Oluşturucu
    private HBox createParamRow(String paramName, String thresholdVal, VBox parentContainer) {
        HBox row = new HBox(10);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        TextField txtParam = new TextField(paramName);
        txtParam.setPromptText("Parametre Adı");
        HBox.setHgrow(txtParam, Priority.ALWAYS); // Genişleyebilsin

        TextField txtThreshold = new TextField(thresholdVal);
        txtThreshold.setPromptText("Eşik (örn: <50)");
        txtThreshold.setPrefWidth(120);

        Button btnRemove = new Button("X");
        btnRemove.setStyle("-fx-background-color: #ffcccc; -fx-text-fill: red; -fx-font-weight: bold; -fx-border-color: red; -fx-border-radius: 3; -fx-background-radius: 3;");
        btnRemove.setOnAction(e -> parentContainer.getChildren().remove(row));

        row.getChildren().addAll(txtParam, txtThreshold, btnRemove);
        return row;
    }

    private void setupTableContextMenus() {
        tablePatients.setRowFactory(tv -> {
            TableRow<Patient> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();
            MenuItem edit = new MenuItem("Bilgileri Düzenle");
            edit.setOnAction(e -> { if (row.getItem() != null) showEditPatientDialog(row.getItem()); });
            MenuItem pres = new MenuItem("Reçeteleri Görüntüle");
            pres.setOnAction(e -> { if (row.getItem() != null) showViewPrescriptionsDialog(row.getItem()); });
            MenuItem charts = new MenuItem("Grafikleri Göster");
            charts.setStyle("-fx-font-weight: bold;");
            charts.setOnAction(e -> { if (row.getItem() != null) showChartsWindow(row.getItem()); });
            menu.getItems().addAll(edit, pres, new SeparatorMenuItem(), charts);
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu)null).otherwise(menu));
            return row;
        });

        tableHistory.setRowFactory(tv -> {
            TableRow<HealthRecord> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();
            MenuItem editRecord = new MenuItem("Kaydı Düzenle");
            editRecord.setOnAction(e -> { if (row.getItem() != null) showEditHistoryDialog(row.getItem()); });
            MenuItem deleteRecord = new MenuItem("Kaydı Sil");
            deleteRecord.setStyle("-fx-text-fill: red;");
            deleteRecord.setOnAction(e -> { if (row.getItem() != null) removeHistoryRecord(row.getItem()); });
            menu.getItems().addAll(editRecord, deleteRecord);
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu)null).otherwise(menu));
            return row;
        });
    }

    private void loadData() {
        diseaseTemplates = DataManager.loadDiseases();
        if (diseaseTemplates.isEmpty()) { createDefaultDiseaseTemplates(); DataManager.saveData(diseaseTemplates, new ArrayList<>()); }
        List<Patient> loadedPatients = DataManager.loadPatients(diseaseTemplates);
        DataManager.loadRecords(loadedPatients); DataManager.loadPrescriptions(loadedPatients);
        patientList.setAll(loadedPatients);
    }

    private void saveData() { DataManager.saveData(diseaseTemplates, new ArrayList<>(patientList)); }

    private void createDefaultDiseaseTemplates() {
        Map<String, String> htCriticals = new HashMap<>(); htCriticals.put("Büyük Tansiyon", ">140"); htCriticals.put("Nabız", ">100");
        diseaseTemplates.add(new DiseaseTemplate("Hipertansiyon", Arrays.asList("Büyük Tansiyon", "Küçük Tansiyon", "Nabız"), Arrays.asList("Amlodipin", "Valsartan"), htCriticals));

        Map<String, String> dmCriticals = new HashMap<>(); dmCriticals.put("Açlık Şekeri (mg/dL)", ">126");
        diseaseTemplates.add(new DiseaseTemplate("Diyabet", Arrays.asList("Açlık Şekeri (mg/dL)", "Tokluk Şekeri (mg/dL)", "İnsülin Dozu"), Arrays.asList("Metformin"), dmCriticals));
    }

    private void loadPatientDetails(Patient patient) {
        this.selectedPatient = patient;
        lblSelectedPatientName.setText(patient.getName() + " " + patient.getSurname());
        tableHistory.getColumns().clear();
        TableColumn<HealthRecord, LocalDate> dateCol = new TableColumn<>("Tarih");
        dateCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDate()));
        dateCol.setMinWidth(120); dateCol.setStyle("-fx-alignment: CENTER;");
        tableHistory.getColumns().add(dateCol);

        for (String param : patient.getDisease().getParameters()) {
            TableColumn<HealthRecord, String> dynamicCol = new TableColumn<>(param);
            dynamicCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue(param)));
            dynamicCol.setMinWidth(150); dynamicCol.setStyle("-fx-alignment: CENTER;");

            String thresholdRule = patient.getDisease().getThreshold(param);

            dynamicCol.setCellFactory(column -> new TableCell<HealthRecord, String>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) { setText(null); setStyle(""); } else {
                        setText(item);
                        String style = "-fx-alignment: CENTER;";
                        if (thresholdRule != null) {
                            try {
                                double val = Double.parseDouble(item.replaceAll("[^0-9.,]", "").replace(",", "."));
                                boolean danger = false;
                                if (thresholdRule.startsWith(">")) {
                                    if (val > Double.parseDouble(thresholdRule.substring(1))) danger = true;
                                } else if (thresholdRule.startsWith("<")) {
                                    if (val < Double.parseDouble(thresholdRule.substring(1))) danger = true;
                                }
                                if (danger) style += "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;";
                            } catch (Exception e) {}
                        }
                        setStyle(style);
                    }
                }
            });
            tableHistory.getColumns().add(dynamicCol);
        }
        TableColumn<HealthRecord, String> noteCol = new TableColumn<>("Doktor Notu");
        noteCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNote()));
        noteCol.setMinWidth(300); noteCol.setStyle("-fx-alignment: CENTER-LEFT;");
        tableHistory.getColumns().add(noteCol);
        tableHistory.setItems(FXCollections.observableArrayList(patient.getHistory()));
        generateInputFields(patient.getDisease()); lblCriticalWarning.setVisible(false);
    }

    private void generateInputFields(DiseaseTemplate disease) {
        dynamicInputContainer.getChildren().clear(); dynamicInputs.clear();
        Label lblDate = new Label("Tarih"); lblDate.setStyle("-fx-font-weight: bold; -fx-text-fill: #6b7280;");
        dynamicDatePicker = new DatePicker(LocalDate.now()); dynamicDatePicker.setMaxWidth(Double.MAX_VALUE);
        dynamicInputContainer.add(lblDate, 0, 0); dynamicInputContainer.add(dynamicDatePicker, 1, 0);
        List<String> params = disease.getParameters();
        int row = 0; int col = 2;
        for (int i = 0; i < params.size(); i++) {
            String paramName = params.get(i); Label lbl = new Label(paramName); lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #6b7280;");
            TextField txt = new TextField(); txt.setPromptText("Değer...");
            if (col > 3) { col = 0; row++; } if (row == 0 && col < 2) col = 2;
            dynamicInputContainer.add(lbl, col, row); dynamicInputContainer.add(txt, col + 1, row);
            dynamicInputs.put(paramName, txt); col += 2;
        }
    }

    private void saveHealthRecord() {
        if (selectedPatient == null) return;
        LocalDate date = dynamicDatePicker.getValue(); String note = txtNotes.getText();
        Map<String, String> values = new HashMap<>();
        for (Map.Entry<String, Control> entry : dynamicInputs.entrySet()) {
            if (entry.getValue() instanceof TextField) values.put(entry.getKey(), ((TextField) entry.getValue()).getText());
        }
        checkCriticalValues(selectedPatient, values);
        selectedPatient.addRecord(new HealthRecord(date, note, values)); saveData(); loadPatientDetails(selectedPatient);
        txtNotes.clear(); dynamicInputs.values().forEach(c -> { if(c instanceof TextField) ((TextField)c).clear(); });
    }

    private void showAddPatientDialog() {
        Dialog<Patient> dialog = new Dialog<>(); dialog.setTitle("Yeni Hasta Ekle");
        ButtonType loginButtonType = new ButtonType("Kaydet", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));
        TextField tc = new TextField(); tc.setPromptText("TC No (11 Hane)"); TextField name = new TextField(); name.setPromptText("Ad");
        TextField surname = new TextField(); surname.setPromptText("Soyad"); TextField age = new TextField(); age.setPromptText("Yaş");
        tc.textProperty().addListener((obs, o, n) -> { if (!n.matches("\\d*")) tc.setText(n.replaceAll("[^\\d]", "")); if(tc.getText().length()>11) tc.setText(tc.getText().substring(0,11)); });
        age.textProperty().addListener((obs, o, n) -> { if (!n.matches("\\d*")) age.setText(n.replaceAll("[^\\d]", "")); });
        ComboBox<String> cbGender = new ComboBox<>(); cbGender.getItems().addAll("Erkek", "Kadın");
        ComboBox<String> cbBlood = new ComboBox<>(); cbBlood.getItems().addAll("A Rh+", "A Rh-", "B Rh+", "B Rh-", "AB Rh+", "AB Rh-", "0 Rh+", "0 Rh-");
        ComboBox<DiseaseTemplate> cbDisease = new ComboBox<>(); cbDisease.setItems(FXCollections.observableArrayList(diseaseTemplates)); cbDisease.getSelectionModel().selectFirst();
        Button btnAddDisease = new Button("+"); btnAddDisease.setOnAction(e -> { DiseaseTemplate nt = showAddDiseaseDialog(); if(nt!=null){ diseaseTemplates.add(nt); cbDisease.setItems(FXCollections.observableArrayList(diseaseTemplates)); cbDisease.getSelectionModel().select(nt); saveData(); } });
        HBox diseaseBox = new HBox(5, cbDisease, btnAddDisease);
        grid.add(new Label("TC:"), 0, 0); grid.add(tc, 1, 0); grid.add(new Label("Ad:"), 0, 1); grid.add(name, 1, 1);
        grid.add(new Label("Soyad:"), 0, 2); grid.add(surname, 1, 2); grid.add(new Label("Cinsiyet:"), 0, 3); grid.add(cbGender, 1, 3);
        grid.add(new Label("Yaş:"), 0, 4); grid.add(age, 1, 4); grid.add(new Label("Kan Grb:"), 0, 5); grid.add(cbBlood, 1, 5);
        grid.add(new Label("Hastalık:"), 0, 6); grid.add(diseaseBox, 1, 6);
        dialog.getDialogPane().setContent(grid);
        final Button btOk = (Button) dialog.getDialogPane().lookupButton(loginButtonType);
        btOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (tc.getText().length() != 11) { showAlert("Hata", "TC 11 hane olmalı!"); event.consume(); return; }
            if (patientList.stream().anyMatch(p -> p.getTcNo().equals(tc.getText()))) { showAlert("Hata", "Bu TC zaten kayıtlı!"); event.consume(); }
        });
        dialog.setResultConverter(btn -> { if (btn == loginButtonType) return new Patient(tc.getText(), name.getText(), surname.getText(), cbGender.getValue(), Integer.parseInt(age.getText()), cbBlood.getValue(), cbDisease.getValue()); return null; });
        Optional<Patient> result = dialog.showAndWait(); result.ifPresent(p -> { patientList.add(p); tablePatients.getSelectionModel().select(p); saveData(); });
    }

    private void showEditPatientDialog(Patient patientToEdit) {
        Dialog<Boolean> dialog = new Dialog<>(); dialog.setTitle("Düzenle"); dialog.setHeaderText("Düzenlenen: " + patientToEdit.getName());
        ButtonType saveBtn = new ButtonType("Güncelle", ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));
        TextField tc = new TextField(patientToEdit.getTcNo()); TextField name = new TextField(patientToEdit.getName());
        TextField surname = new TextField(patientToEdit.getSurname()); TextField age = new TextField(String.valueOf(patientToEdit.getAge()));
        tc.textProperty().addListener((obs, o, n) -> { if (!n.matches("\\d*")) tc.setText(n.replaceAll("[^\\d]", "")); if(tc.getText().length()>11) tc.setText(tc.getText().substring(0,11)); });
        age.textProperty().addListener((obs, o, n) -> { if (!n.matches("\\d*")) age.setText(n.replaceAll("[^\\d]", "")); });
        ComboBox<String> cbGender = new ComboBox<>(); cbGender.getItems().addAll("Erkek", "Kadın"); cbGender.setValue(patientToEdit.getGender());
        ComboBox<String> cbBlood = new ComboBox<>(); cbBlood.getItems().addAll("A Rh+", "A Rh-", "B Rh+", "B Rh-", "AB Rh+", "AB Rh-", "0 Rh+", "0 Rh-"); cbBlood.setValue(patientToEdit.getBloodGroup());
        ComboBox<DiseaseTemplate> cbDisease = new ComboBox<>(); cbDisease.setItems(FXCollections.observableArrayList(diseaseTemplates));
        for(DiseaseTemplate dt : diseaseTemplates) if(dt.getName().equals(patientToEdit.getDiseaseName())) { cbDisease.getSelectionModel().select(dt); break; }
        grid.add(new Label("TC:"), 0, 0); grid.add(tc, 1, 0); grid.add(new Label("Ad:"), 0, 1); grid.add(name, 1, 1);
        grid.add(new Label("Soyad:"), 0, 2); grid.add(surname, 1, 2); grid.add(new Label("Cinsiyet:"), 0, 3); grid.add(cbGender, 1, 3);
        grid.add(new Label("Yaş:"), 0, 4); grid.add(age, 1, 4); grid.add(new Label("Kan Grb:"), 0, 5); grid.add(cbBlood, 1, 5);
        grid.add(new Label("Hastalık:"), 0, 6); grid.add(cbDisease, 1, 6);
        dialog.getDialogPane().setContent(grid);
        final Button btOk = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        btOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (tc.getText().length() != 11) { showAlert("Hata", "TC 11 hane olmalı!"); event.consume(); return; }
            if (patientList.stream().anyMatch(p -> p.getTcNo().equals(tc.getText()) && !p.equals(patientToEdit))) { showAlert("Hata", "Bu TC başka hastada kayıtlı!"); event.consume(); }
        });
        dialog.setResultConverter(btn -> { if (btn == saveBtn) { patientToEdit.setTcNo(tc.getText()); patientToEdit.setName(name.getText()); patientToEdit.setSurname(surname.getText()); patientToEdit.setGender(cbGender.getValue()); patientToEdit.setAge(Integer.parseInt(age.getText())); patientToEdit.setBloodGroup(cbBlood.getValue()); patientToEdit.setDisease(cbDisease.getValue()); return true; } return null; });
        Optional<Boolean> res = dialog.showAndWait(); res.ifPresent(updated -> { if(updated) { tablePatients.refresh(); if(selectedPatient!=null && selectedPatient.equals(patientToEdit)) loadPatientDetails(patientToEdit); saveData(); }});
    }

    private void removeSelectedPatient() {
        Patient selected = tablePatients.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION); alert.setTitle("Silme Onayı"); alert.setHeaderText("Silmek istediğinize emin misiniz?");
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) { patientList.remove(selected); saveData(); lblSelectedPatientName.setText("-- Seçim Yapılmadı --"); tableHistory.getColumns().clear(); dynamicInputContainer.getChildren().clear(); }
        } else showAlert("Uyarı", "Lütfen bir hasta seçiniz.");
    }

    private DiseaseTemplate showAddDiseaseDialog() {
        Dialog<DiseaseTemplate> dialog = new Dialog<>();
        dialog.setTitle("Yeni Hastalık Tanımla");
        dialog.setHeaderText("Hastalık parametrelerini ve kritik eşikleri belirleyin.");
        ButtonType saveBtn = new ButtonType("Oluştur", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField txtDiseaseName = new TextField(); txtDiseaseName.setPromptText("Örn: Böbrek Yetmezliği");
        TextArea txtParams = new TextArea(); txtParams.setPromptText("Parametreler (Satır satır)..."); txtParams.setPrefHeight(60);
        TextArea txtMeds = new TextArea(); txtMeds.setPromptText("Varsayılan İlaçlar (Satır satır)..."); txtMeds.setPrefHeight(60);

        Label lblCrit = new Label("Kritik Eşik Tanımlama:"); lblCrit.setStyle("-fx-font-weight: bold;");
        ListView<String> listThresholds = new ListView<>(); listThresholds.setPrefHeight(100);
        TextField txtParamName = new TextField(); txtParamName.setPromptText("Parametre");
        ComboBox<String> cbOperator = new ComboBox<>(); cbOperator.getItems().addAll(">", "<"); cbOperator.setValue(">");
        TextField txtValue = new TextField(); txtValue.setPromptText("Değer"); txtValue.setPrefWidth(80);

        Button btnAddThreshold = new Button("Ekle");
        btnAddThreshold.setOnAction(e -> {
            if (!txtParamName.getText().isEmpty() && !txtValue.getText().isEmpty()) {
                String rule = txtParamName.getText() + ":" + cbOperator.getValue() + txtValue.getText();
                listThresholds.getItems().add(rule); txtParamName.clear(); txtValue.clear();
            }
        });
        HBox addBox = new HBox(5, txtParamName, cbOperator, txtValue, btnAddThreshold);
        grid.add(new Label("Hastalık Adı:"), 0, 0); grid.add(txtDiseaseName, 1, 0);
        grid.add(new Label("Parametreler:"), 0, 1); grid.add(txtParams, 1, 1);
        grid.add(new Label("İlaçlar:"), 0, 2); grid.add(txtMeds, 1, 2);
        grid.add(lblCrit, 0, 3); VBox critBox = new VBox(5, listThresholds, addBox); grid.add(critBox, 1, 3);
        dialog.getDialogPane().setContent(grid);

        final Button btOk = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        btOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (txtDiseaseName.getText().trim().isEmpty()) { showAlert("Hata", "İsim giriniz."); event.consume(); return; }
            if (diseaseTemplates.stream().anyMatch(d -> d.getName().equalsIgnoreCase(txtDiseaseName.getText().trim()))) { showAlert("Hata", "Zaten mevcut!"); event.consume(); }
        });

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                List<String> params = Arrays.stream(txtParams.getText().split("\n")).map(String::trim).filter(s->!s.isEmpty()).collect(Collectors.toList());
                List<String> meds = Arrays.stream(txtMeds.getText().split("\n")).map(String::trim).filter(s->!s.isEmpty()).collect(Collectors.toList());
                Map<String, String> crits = new HashMap<>();
                for(String s : listThresholds.getItems()) { String[] p = s.split(":"); if(p.length == 2) crits.put(p[0].trim(), p[1].trim()); }
                return new DiseaseTemplate(txtDiseaseName.getText().trim(), params, meds, crits);
            } return null;
        });
        return dialog.showAndWait().orElse(null);
    }

    private void showAddPrescriptionDialog() {
        Patient patient = tablePatients.getSelectionModel().getSelectedItem();
        if (patient == null) { showAlert("Uyarı", "Lütfen önce reçete yazılacak hastayı seçiniz."); return; }
        Dialog<Prescription> dialog = new Dialog<>(); dialog.setTitle("Reçete Oluştur - " + patient.getName()); dialog.setHeaderText("Hastalık: " + patient.getDiseaseName());
        ButtonType saveBtn = new ButtonType("Reçeteyi Kaydet", ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        VBox content = new VBox(10); content.setPadding(new Insets(20));
        HBox dateBox = new HBox(10); VBox startBox = new VBox(5); startBox.getChildren().addAll(new Label("Başlangıç:"), new DatePicker(LocalDate.now()));
        VBox endBox = new VBox(5); endBox.getChildren().addAll(new Label("Bitiş:"), new DatePicker(LocalDate.now().plusDays(7)));
        DatePicker dpStart = (DatePicker) startBox.getChildren().get(1); DatePicker dpEnd = (DatePicker) endBox.getChildren().get(1);
        dateBox.getChildren().addAll(startBox, endBox);
        Label lblMeds = new Label("İlaç Seçimi:"); lblMeds.setStyle("-fx-font-weight: bold;");
        VBox medsBox = new VBox(5); List<CheckBox> checkBoxes = new ArrayList<>();
        List<String> availableMeds = patient.getDisease().getDefaultMedicines();
        for (String med : availableMeds) { CheckBox cb = new CheckBox(med); checkBoxes.add(cb); medsBox.getChildren().add(cb); }
        HBox addMedBox = new HBox(10); TextField txtNewMed = new TextField(); txtNewMed.setPromptText("Listede olmayan ilaç..."); Button btnAddMed = new Button("Listeye Ekle");
        btnAddMed.setOnAction(e -> {
            String newMedName = txtNewMed.getText().trim();
            if (!newMedName.isEmpty()) {
                CheckBox newCb = new CheckBox(newMedName); newCb.setSelected(true); checkBoxes.add(newCb); medsBox.getChildren().add(newCb);
                List<String> currentMeds = patient.getDisease().getDefaultMedicines();
                if (currentMeds.stream().noneMatch(m -> m.equalsIgnoreCase(newMedName))) { currentMeds.add(newMedName); saveData(); }
                txtNewMed.clear();
            }
        }); addMedBox.getChildren().addAll(txtNewMed, btnAddMed);
        Label lblNote = new Label("Doktor Notu:"); lblNote.setStyle("-fx-font-weight: bold;"); TextArea txtNote = new TextArea(); txtNote.setPromptText("Kullanım talimatı..."); txtNote.setPrefHeight(80);
        content.getChildren().addAll(dateBox, lblMeds, medsBox, addMedBox, lblNote, txtNote);
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(btn -> { if (btn == saveBtn) { List<String> selectedMeds = new ArrayList<>(); for (CheckBox cb : checkBoxes) if (cb.isSelected()) selectedMeds.add(cb.getText()); return new Prescription(dpStart.getValue(), dpEnd.getValue(), selectedMeds, txtNote.getText()); } return null; });
        Optional<Prescription> result = dialog.showAndWait(); result.ifPresent(p -> { patient.addPrescription(p); saveData(); showAlert("Başarılı", "Reçete kaydedildi."); });
    }

    private void showViewPrescriptionsDialog(Patient patient) {
        Dialog<Void> dialog = new Dialog<>(); dialog.setTitle("Reçete Geçmişi - " + patient.getName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ListView<String> listView = new ListView<>();
        if (patient.getPrescriptions().isEmpty()) listView.getItems().add("Henüz kayıtlı reçete yok.");
        else for (Prescription p : patient.getPrescriptions()) listView.getItems().add(String.format("TARİH ARALIĞI: %s - %s\nİLAÇLAR: %s\nNOT: %s\n-------------------", p.getStartDate(), p.getEndDate(), String.join(", ", p.getMedicines()), p.getDoctorNote()));
        listView.setPrefSize(450, 350); dialog.getDialogPane().setContent(listView); dialog.showAndWait();
    }

    private void showEditHistoryDialog(HealthRecord record) {
        Dialog<Boolean> dialog = new Dialog<>(); dialog.setTitle("Kayıt Düzenle"); dialog.setHeaderText("Tarih: " + record.getDate());
        ButtonType saveBtn = new ButtonType("Güncelle", ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        DatePicker dp = new DatePicker(record.getDate()); grid.add(new Label("Tarih:"), 0, 0); grid.add(dp, 1, 0);
        List<String> params = selectedPatient.getDisease().getParameters(); Map<String, TextField> editInputs = new HashMap<>();
        int row = 1; for (String param : params) { TextField txt = new TextField(record.getValue(param)); grid.add(new Label(param + ":"), 0, row); grid.add(txt, 1, row); editInputs.put(param, txt); row++; }
        TextArea txtNote = new TextArea(record.getNote()); txtNote.setPrefHeight(60); grid.add(new Label("Not:"), 0, row); grid.add(txtNote, 1, row);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> { if (btn == saveBtn) { record.setDate(dp.getValue()); record.setNote(txtNote.getText()); for (Map.Entry<String, TextField> entry : editInputs.entrySet()) record.getValues().put(entry.getKey(), entry.getValue().getText()); return true; } return null; });
        Optional<Boolean> result = dialog.showAndWait(); result.ifPresent(updated -> { if (updated) { tableHistory.refresh(); saveData(); } });
    }

    private void removeHistoryRecord(HealthRecord record) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION); alert.setTitle("Kaydı Sil"); alert.setHeaderText("Bu kayıt silinecek, emin misiniz?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) { selectedPatient.getHistory().remove(record); tableHistory.getItems().remove(record); saveData(); }
    }

    private void showChartsWindow(Patient patient) {
        Stage stage = new Stage(); stage.setTitle("Grafik Takibi - " + patient.getName() + " " + patient.getSurname());
        VBox root = new VBox(20); root.setPadding(new Insets(20)); root.setStyle("-fx-background-color: #f4f4f4;");
        Label title = new Label(patient.getDiseaseName() + " - Değer Analizi"); title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        ScrollPane scrollPane = new ScrollPane(); scrollPane.setFitToWidth(true); VBox chartsContainer = new VBox(30); chartsContainer.setPadding(new Insets(10));
        List<String> parameters = patient.getDisease().getParameters(); boolean hasChart = false; DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");
        List<HealthRecord> sortedHistory = new ArrayList<>(patient.getHistory()); sortedHistory.sort(Comparator.comparing(HealthRecord::getDate));
        for (String param : parameters) {
            XYChart.Series<String, Number> series = new XYChart.Series<>(); series.setName(param);
            for (HealthRecord record : sortedHistory) {
                String valueStr = record.getValue(param);
                if (!valueStr.equals("-") && !valueStr.isEmpty()) {
                    try {
                        String cleanVal = valueStr.replaceAll("[^0-9.,]", "").replace(",", ".");
                        if (!cleanVal.isEmpty()) { double value = Double.parseDouble(cleanVal); String dateStr = record.getDate().format(formatter); series.getData().add(new XYChart.Data<>(dateStr, value)); }
                    } catch (NumberFormatException e) {}
                }
            }
            if (!series.getData().isEmpty()) {
                hasChart = true; CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Tarih"); NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Değer");
                LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis); lineChart.getData().add(series); lineChart.setTitle(param); lineChart.setPrefHeight(300); lineChart.setLegendVisible(false);
                lineChart.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);"); chartsContainer.getChildren().add(lineChart);
            }
        }
        if (!hasChart) { Label noDataLabel = new Label("Grafik oluşturulacak sayısal veri bulunamadı."); noDataLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-alignment: CENTER;"); chartsContainer.getChildren().add(noDataLabel); }
        scrollPane.setContent(chartsContainer); root.getChildren().addAll(title, scrollPane);
        Scene scene = new Scene(root, 800, 600); stage.setScene(scene); stage.show();
    }
// ---- PDF ÇIKTISI ----
    private void exportToPdf() {
        if (selectedPatient == null) { showAlert("Uyarı", "Lütfen raporu alınacak hastayı seçiniz."); return; }
        FileChooser fileChooser = new FileChooser(); fileChooser.setTitle("Raporu Kaydet");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Dosyası", "*.pdf"));
        fileChooser.setInitialFileName(selectedPatient.getName() + "_" + selectedPatient.getSurname() + "_Rapor.pdf");
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                Document document = new Document(PageSize.A4); PdfWriter.getInstance(document, new FileOutputStream(file)); document.open();
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY); Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK); Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
                try { BaseFont bf = BaseFont.createFont("c:/windows/fonts/arial.ttf", "Cp1254", BaseFont.EMBEDDED); titleFont = new Font(bf, 18, Font.BOLD); normalFont = new Font(bf, 12, Font.NORMAL); boldFont = new Font(bf, 12, Font.BOLD); } catch (Exception ignored) {}
                Paragraph title = new Paragraph("HASTA SEYİR RAPORU", titleFont); title.setAlignment(Element.ALIGN_CENTER); title.setSpacingAfter(20); document.add(title);
                document.add(new Paragraph("TC Kimlik No: " + selectedPatient.getTcNo(), normalFont)); document.add(new Paragraph("Adı Soyadı: " + selectedPatient.getName() + " " + selectedPatient.getSurname(), normalFont));
                document.add(new Paragraph("Cinsiyet: " + selectedPatient.getGender() + " | Yaş: " + selectedPatient.getAge(), normalFont)); document.add(new Paragraph("Kan Grubu: " + selectedPatient.getBloodGroup(), normalFont));
                document.add(new Paragraph("Hastalık Tanısı: " + selectedPatient.getDiseaseName(), boldFont)); document.add(new Paragraph(" ", normalFont));
                List<String> params = selectedPatient.getDisease().getParameters(); PdfPTable table = new PdfPTable(2 + params.size()); table.setWidthPercentage(100); table.setSpacingBefore(10f);
                addCell(table, "Tarih", boldFont, true); for (String param : params) addCell(table, param, boldFont, true); addCell(table, "Doktor Notu", boldFont, true);
                for (HealthRecord record : selectedPatient.getHistory()) { addCell(table, record.getDate().toString(), normalFont, false); for (String param : params) addCell(table, record.getValue(param), normalFont, false); addCell(table, record.getNote(), normalFont, false); }
                document.add(table); document.add(new Paragraph("\nRapor Tarihi: " + LocalDate.now(), normalFont)); document.close(); showAlert("Başarılı", "Rapor kaydedildi.");
            } catch (Exception e) { e.printStackTrace(); showAlert("Hata", "PDF hatası: " + e.getMessage()); }
        }
    }
    private void addCell(PdfPTable table, String text, Font font, boolean isHeader) { PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font)); cell.setPadding(5); if (isHeader) { cell.setBackgroundColor(BaseColor.LIGHT_GRAY); cell.setHorizontalAlignment(Element.ALIGN_CENTER); } table.addCell(cell); }

    private void toggleMenu() { Duration d = Duration.millis(250); Timeline t = new Timeline(); t.getKeyFrames().add(new KeyFrame(d, new KeyValue(sidebarPane.prefWidthProperty(), isMenuOpen ? 0 : 240), new KeyValue(sidebarPane.minWidthProperty(), isMenuOpen ? 0 : 240))); t.play(); isMenuOpen = !isMenuOpen; }
    private void showAlert(String title, String content) { Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait(); }
}