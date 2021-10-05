package co.flickpost.admin.helpers;

import co.flickpost.admin.models.Shipment;
import co.flickpost.admin.models.Package;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelHelper {

    final static DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    final static DateTimeFormatter packageDateFormat = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");

    private final static String PACKAGE_SHEET_NAME = "Packages";
    private final static String[] PACKAGE_COLUMNS = new String[]{"Hub", "Tracking Number", "Audited Length(cm)", "Audited Width(cm)",
            "Audited Height(cm)", "Audited Actual Weight(kg)", "Date and Time", "Image File",
            "Upload Status", "Remarks", "Audited Volumetric Weight(kg)", "Chargeable Weight(kg)"};

    public static List<Package> readPackages(HSSFWorkbook workbook, String company) throws Exception {
        List<Package> uploadedPackages = new ArrayList<>();

        if(workbook == null || workbook.getNumberOfSheets() < 1){
            return uploadedPackages;
        }

        HSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        if(rowIterator == null){
            return uploadedPackages;
        }

        rowIterator.next(); //Ignore headers
        while(rowIterator.hasNext()){
            Package newPackage = new Package();
            Row row = rowIterator.next();
            if(row == null){
                continue;
            }

            Iterator<Cell> cellIterator = row.cellIterator();
            if(cellIterator == null){
                continue;
            }

            int cellIndex = 0;
            while(cellIterator.hasNext()){
                Cell cell = cellIterator.next();
                updatePackage(newPackage, cell, cellIndex, company);
                cellIndex++;
            }

            if(StringUtils.isNotEmpty(newPackage.getTrackingNumber())){
                newPackage.setImageInfos(null);
                newPackage.setHid(null);
                PackageHelper.updateVolumetricWeight(newPackage);
                PackageHelper.updateChargeableWeight(newPackage);
                uploadedPackages.add(newPackage);
            }
        }

        return uploadedPackages;

    }

    public static List<Shipment> readShipments(HSSFWorkbook workbook) throws Exception {
        List<Shipment> recordShipments = new ArrayList<>();

        if(workbook == null || workbook.getNumberOfSheets() < 1){
            return recordShipments;
        }

        HSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        if(rowIterator == null){
            return recordShipments;
        }

        rowIterator.next(); //Ignore headers
        while(rowIterator.hasNext()){
            Shipment newShipment = new Shipment();
            Row row = rowIterator.next();
            if(row == null){
                continue;
            }

            Iterator<Cell> cellIterator = row.cellIterator();
            if(cellIterator == null){
                continue;
            }

            cellIterator.next(); //Ignore SI NO
            int cellIndex = 0;
            while(cellIterator.hasNext()){
                if(cellIndex == 26) {
                    cellIterator.next();
                    cellIndex++;
                    continue;
                }//skip duplicate weight

                Cell cell = cellIterator.next();
                updateShipment(newShipment, cell, cellIndex);
                cellIndex++;
            }

            if(StringUtils.isNotEmpty(newShipment.getTrackingNumber())){
                recordShipments.add(newShipment);
            }
        }

        return recordShipments;

    }

    public static byte[] packagesToExcel(List<Package> packages) {

        byte[] bytes = null;

        try (Workbook workbook = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(PACKAGE_SHEET_NAME);

            // Header
            Row headerRow = sheet.createRow(0);

            for (int col = 0; col < PACKAGE_COLUMNS.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(PACKAGE_COLUMNS[col]);
            }

            int rowIdx = 1;
            for (Package pkg : packages) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(pkg.getHub());
                row.createCell(1).setCellValue(pkg.getTrackingNumber());
                row.createCell(2).setCellValue(pkg.getAuditedLength().doubleValue());
                row.createCell(3).setCellValue(pkg.getAuditedWidth().doubleValue());
                row.createCell(4).setCellValue(pkg.getAuditedHeight().doubleValue());
                row.createCell(5).setCellValue(pkg.getAuditedHeight().doubleValue());
                row.createCell(6).setCellValue(pkg.getDateTime());
                row.createCell(7).setCellValue("");
                row.createCell(8).setCellValue("Success");
                row.createCell(9).setCellValue(pkg.getHid());
                row.createCell(10).setCellValue(pkg.getAuditedVolumetricWeight().doubleValue());
                row.createCell(11).setCellValue(pkg.getChargeableWeight().doubleValue());
            }

            workbook.write(out);
            out.close();
            bytes = out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
        }

        return bytes;
    }

    private static void updatePackage(Package pkg, Cell cell, int cellIndex, String company) throws Exception {
        BigDecimal value = null;
        String stringVal;

        if(cell.getCellType() == CellType.NUMERIC){
            value = new BigDecimal(cell.getNumericCellValue());
            stringVal = value.toString();
        } else{
            stringVal = cell.getStringCellValue();
        }

        switch(cellIndex){
            case 0:
                if(StringUtils.isEmpty(stringVal)){
                    stringVal = company;
                }
                pkg.setHub(stringVal);
                break;
            case 1:
                pkg.setTrackingNumber(stringVal);
                break;
            case 2:
                pkg.setAuditedLength(value.setScale(2, RoundingMode.UP));
                break;
            case 3:
                pkg.setAuditedWidth(value.setScale(2, RoundingMode.UP));
                break;
            case 4:
                pkg.setAuditedHeight(value.setScale(2, RoundingMode.UP));
                break;
            case 5:
                pkg.setAuditedWeight(value.setScale(2, RoundingMode.UP));
                break;
            case 6:
                if(StringUtils.isEmpty(stringVal)){
                    pkg.setDateTime(LocalDateTime.now());
                } else{
                    pkg.setDateTime(getDateVal(stringVal, false));
                }
                break;
            case 7:
                //If image is hosted, can use to obtain url TODO
                break;
            case 8:
                //If need to keep track of "Upload Status"
                break;
            case 9:
                if(StringUtils.isEmpty(stringVal)){
                    pkg.setHid(null);
                } else{
                    pkg.setHid(stringVal);
                }
                break;
            case 10:
                if(value != null) {
                    pkg.setAuditedVolumetricWeight(value.setScale(2, RoundingMode.UP));
                }
                break;
            case 11:
                if(value != null) {
                    pkg.setChargeableWeight(value.setScale(1, RoundingMode.UP));
                }
                break;
        }
    }

    private static void updateShipment(Shipment shipment, Cell cell, int cellIndex) throws Exception {
        BigDecimal value = null;
        Long longVal = null;
        Integer intVal = null;
        String stringVal;

        if(cell.getCellType() == CellType.NUMERIC){
            value = new BigDecimal(cell.getNumericCellValue());
            longVal = (long)Math.floor(cell.getNumericCellValue());
            intVal = longVal.intValue();
            stringVal = longVal.toString();
        } else{
            stringVal = cell.getStringCellValue();
        }

        switch(cellIndex){
            case 0:
                shipment.setTrackingNumber(stringVal);
                break;
            case 1:
                shipment.setReferenceNumber(stringVal);
                break;
            case 2:
                shipment.setCustomerSystemNumber(longVal);
                break;
            case 3:
                shipment.setCustomerAccountNumber(longVal);
                break;
            case 4:
                shipment.setCustomerName(stringVal);
                break;
            case 5:
                shipment.setSenderName(stringVal);
                break;
            case 6:
                shipment.setSenderPhone(stringVal);
                break;
            case 7:
                shipment.setSenderStreetAddress(stringVal);
                break;
            case 8:
                shipment.setSenderCity(stringVal);
                break;
            case 9:
                shipment.setSenderState(stringVal);
                break;
            case 10:
                shipment.setSenderCountry(stringVal);
                break;
            case 11:
                shipment.setSenderCode(longVal);
                break;
            case 12:
                shipment.setReceiverName(stringVal);
                break;
            case 13:
                shipment.setReceiverEmail(stringVal);
                break;
            case 14:
                shipment.setReceiverPhone(stringVal);
                break;
            case 15:
                shipment.setReceiverStreetAddress(stringVal);
                break;
            case 16:
                shipment.setReceiverCity(stringVal);
                break;
            case 17:
                shipment.setReceiverState(stringVal);
                break;
            case 18:
                shipment.setReceiverCountry(stringVal);
                break;
            case 19:
                shipment.setReceiverCode(longVal);
                break;
            case 20:
                shipment.setItemsCount(intVal);
                break;
            case 21:
                shipment.setDescription(stringVal);
                break;
            case 22:
                shipment.setQuantity(intVal);
                break;
            case 23:
                shipment.setWeight(value);
                break;
            case 24:
                shipment.setDeclaredValue(value);
                break;
            case 25:
                shipment.setPickupInstructions(stringVal);
                break;
            case 26:
                break;
            case 27:
                shipment.setHeight(value);
                break;
            case 28:
                shipment.setWidth(value);
                break;
            case 29:
                shipment.setScale(value);
                break;
            case 30:
                shipment.setDimensionalWeight(value);
                break;
            case 31:
                shipment.setType(stringVal);
                break;
            case 32:
                shipment.setMode(stringVal);
                break;
            case 33:
                shipment.setShipmentStatus(stringVal);
                break;
            case 34:
                shipment.setShippingCost(stringVal);
                break;
            case 35:
                shipment.setCostBreakdown(stringVal);
                break;
            case 36:
                shipment.setPaymentStatus(stringVal);
                break;
            case 37:
                shipment.setBookingDate(getDateVal(stringVal, true));
                break;
            case 38:
                shipment.setDeliveryDate(getDateVal(stringVal, true));
                break;
        }
    }

    private static LocalDateTime getDateVal(String dateString, boolean isShipment) throws Exception {
        if(StringUtils.isEmpty(dateString)) return null;

        return isShipment? LocalDate.parse(dateString, sdf).atStartOfDay() : LocalDateTime.parse(dateString, packageDateFormat);
    }

}
