package charlie.gtalent_spring_boot_260801.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;
import java.util.function.Function;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import charlie.gtalent_spring_boot_260801.entity.Member;
import charlie.gtalent_spring_boot_260801.repository.MemberRepository;
import charlie.gtalent_spring_boot_260801.response.ExcelImportError;
import charlie.gtalent_spring_boot_260801.response.ExcelImportResponse;

@Service
public class ExcelService {

    private static final int MAX_ROWS = 100000;
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    public record MemberQuery(
        String account,
        String name,
        Byte gender,
        Byte status
) {}

    private static final String[] HEADERS = {
        "帳號", "姓名", "性別", "Email", "狀態", "密碼"
    };

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"
        );

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public ExcelService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder) {

        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 可匯出的欄位：key -> (標題, 取值)，順序即預設匯出順序
    private static final Map<String, ExportColumn> EXPORT_COLUMNS = new LinkedHashMap<>();

    private record ExportColumn(String title, Function<Member, Object> getter) {}

    static {
        EXPORT_COLUMNS.put("id", new ExportColumn("ID", Member::getId));
        EXPORT_COLUMNS.put("name", new ExportColumn("姓名", Member::getName));
        EXPORT_COLUMNS.put("account", new ExportColumn("帳號", Member::getAccount));
        EXPORT_COLUMNS.put("gender", new ExportColumn("性別", Member::getGender));
        EXPORT_COLUMNS.put("email", new ExportColumn("Email", Member::getEmail));
        EXPORT_COLUMNS.put("status", new ExportColumn("狀態", Member::getStatus));
    }

       public byte[] exportMembers(MemberQuery query, List<String> selected) throws IOException {

       List<ExportColumn> columns = new ArrayList<>();

       if (selected == null || selected.isEmpty()) {
           columns.addAll(EXPORT_COLUMNS.values());
       } else {
           // 依固定順序輸出，並拒絕未知欄位
           for (String key : selected) {
               if (!EXPORT_COLUMNS.containsKey(key)) {
                   throw new IllegalArgumentException("不支援的匯出欄位：" + key);
               }
           }
           EXPORT_COLUMNS.forEach((key, col) -> {
               if (selected.contains(key)) {
                   columns.add(col);
               }
           });
       }


       List<Member> members =
            memberRepository.search(
                    query.account(),
                    query.name(),
                    query.gender(),
                    query.status()
            );


       try (Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream()) {


           Sheet sheet = workbook.createSheet("會員資料");
           Sheet sheet2 = workbook.createSheet("匯入說明");


           Row header = sheet.createRow(0);

           for (int i = 0; i < columns.size(); i++) {
               header.createCell(i).setCellValue(columns.get(i).title());
           }


           int rowNum = 1;


           for (Member member : members) {
               Row row = sheet.createRow(rowNum++);

               for (int i = 0; i < columns.size(); i++) {
                   Object value = columns.get(i).getter().apply(member);
                   Cell cell = row.createCell(i);

                   if (value instanceof Number n) {
                       cell.setCellValue(n.doubleValue());
                   } else {
                       cell.setCellValue(value == null ? "" : value.toString());
                   }
               }
           }


           for (int i = 0; i < columns.size(); i++) {
               sheet.autoSizeColumn(i);
           }


           workbook.write(out);
           return out.toByteArray();
       }
   }


    private record ImportRow(
        int rowNumber,
        String account,
        String name,
        Byte gender,
        String email,
        Byte status,
        String password
    ) {}

    @Transactional(rollbackFor = Exception.class)
    public ExcelImportResponse importMembers(MultipartFile file)
            throws IOException {
        System.out.println("importMembers() called");
        // 1. 檢查檔案
        if (file == null || file.isEmpty()) {
            return failure("請選擇 Excel 檔案", 0, List.of());
        }

        String filename = file.getOriginalFilename();

        if (filename == null ||
            !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            return failure("只支援 .xlsx 格式", 0, List.of());
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return failure("檔案不可超過 5MB", 0, List.of());
        }

        List<ExcelImportError> errors = new ArrayList<>();
        List<ImportRow> rows = new ArrayList<>();
        Set<String> seenAccounts = new HashSet<>();

        int total = 0;

        // 2. 讀取 Excel
        try (
            InputStream input = file.getInputStream();
            Workbook workbook = WorkbookFactory.create(input)
        ) {

            if (workbook.getNumberOfSheets() == 0) {
                return failure("Excel 沒有工作表", 0, List.of());
            }

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            // 3. 驗證標題
            Row header = sheet.getRow(0);

            if (header == null) {
                return failure("Excel 沒有標題列", 0, List.of());
            }

            for (int i = 0; i < HEADERS.length; i++) {

                String actual = cellValue(header, i, formatter);

                if (!HEADERS[i].equals(actual)) {
                    return failure(
                        "第 " + (i + 1) +
                        " 欄標題必須是：" + HEADERS[i],
                        0,
                        List.of()
                    );
                }
            }

            if (sheet.getLastRowNum() > MAX_ROWS) {
                return failure(
                    "最多只能匯入 10000 筆",
                    0,
                    List.of()
                );
            }

            // 4. 逐列解析
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                int excelRow = i + 1;

                // 不接受公式
                boolean hasFormula = false;

                for (int j = 0; j < HEADERS.length; j++) {
                    Cell cell = row.getCell(j);

                    if (cell != null &&
                        cell.getCellType() == CellType.FORMULA) {
                        hasFormula = true;
                        break;
                    }
                }

                if (hasFormula) {
                    total++;
                    errors.add(new ExcelImportError(
                        excelRow, "row",
                        "不允許使用 Excel 公式"
                    ));
                    continue;
                }

                if (isEmptyRow(row, formatter)) {
                    continue;
                }

                total++;

                String account = cellValue(row, 0, formatter);
                String name = cellValue(row, 1, formatter);
                String genderText = cellValue(row, 2, formatter);
                String email = cellValue(row, 3, formatter);
                String statusText = cellValue(row, 4, formatter);
                String password = rawCellValue(row, 5, formatter);

                int previousErrors = errors.size();

                // 帳號
                if (account.isBlank() || account.length() > 64) {
                    errors.add(new ExcelImportError(
                        excelRow, "account",
                        "帳號必填且最多64字元"
                    ));
                } else if (!seenAccounts.add(account)) {
                    errors.add(new ExcelImportError(
                        excelRow, "account",
                        "Excel內帳號重複"
                    ));
                }

                // 姓名
                if (name.isBlank() || name.length() > 64) {
                    errors.add(new ExcelImportError(
                        excelRow, "name",
                        "姓名必填且最多64字元"
                    ));
                }

                // 性別
                if (!genderText.equals("1") &&
                    !genderText.equals("2")) {
                    errors.add(new ExcelImportError(
                        excelRow, "gender",
                        "性別只能是1或2"
                    ));
                }

                // Email
                if (email.length() > 128 ||
                    (!email.isBlank() &&
                     !EMAIL_PATTERN.matcher(email).matches())) {
                    errors.add(new ExcelImportError(
                        excelRow, "email",
                        "Email格式錯誤"
                    ));
                }

                // 狀態
                if (!statusText.equals("0") &&
                    !statusText.equals("1")) {
                    errors.add(new ExcelImportError(
                        excelRow, "status",
                        "狀態只能是0或1"
                    ));
                }

                // 密碼
                if (!password.isEmpty()) {
                    int bytes = password.getBytes(
                        StandardCharsets.UTF_8
                    ).length;

                    if (password.isBlank() ||
                        password.length() < 8 ||
                        bytes > 72) {
                        errors.add(new ExcelImportError(
                            excelRow, "password",
                            "密碼至少8字元且UTF-8不可超過72位元組"
                        ));
                    }
                }

                if (errors.size() > previousErrors) {
                    continue;
                }

                rows.add(new ImportRow(
                    excelRow,
                    account,
                    name,
                    Byte.parseByte(genderText),
                    email,
                    Byte.parseByte(statusText),
                    password
                ));
            }

        } catch (EncryptedDocumentException ex) {
            return failure("不支援加密Excel", total, List.of());

        } catch (NotOfficeXmlFileException ex) {
            return failure("不是有效的xlsx檔案", total, List.of());
        }

        // 5. 驗證失敗不寫入
        if (!errors.isEmpty()) {
            return failure(
                "Excel驗證失敗，未寫入資料",
                total,
                errors
            );
        }

        if (rows.isEmpty()) {
            return failure("沒有可匯入資料", 0, List.of());
        }

        // 6. 查詢現有帳號
        Map<String, Member> existing =
        memberRepository.findAll()
            .stream()
            .collect(Collectors.toMap(
                Member::getAccount,
                Function.identity()
            ));
        
        // 7. 檢查新會員密碼及軟刪除
        for (ImportRow data : rows) {

            Member member = existing.get(data.account());

            if (member != null &&
                member.getDeletedAt() != null) {
                errors.add(new ExcelImportError(
                    data.rowNumber(),
                    "account",
                    "帳號已軟刪除，禁止更新"
                ));
            }

            if (member == null && data.password().isEmpty()) {
                errors.add(new ExcelImportError(
                    data.rowNumber(),
                    "password",
                    "新增會員必須填寫密碼"
                ));
            }
        }

        if (!errors.isEmpty()) {
            return failure(
                "Excel驗證失敗，未寫入資料",
                total,
                errors
            );
        }

        // 8. 新增或更新
        int inserted = 0;
        int updated = 0;

        List<Member> saveList =
            new ArrayList<>(rows.size());

        String encodedDefaultPassword =
            passwordEncoder.encode("Pass2026!");

        for (ImportRow data : rows) {

            Member member = existing.get(data.account());
            
            if (member == null) {

                member = new Member(
                data.name(),
                data.gender(),
                data.account(),
                data.email().isBlank()
                    ? null : data.email(),
                encodedDefaultPassword
            );

                member.setStatus(data.status());
                inserted++;

            } else {

                member.setName(data.name());
                member.setGender(data.gender());
                member.setEmail(
                    data.email().isBlank()
                        ? null : data.email()
                );
                member.setStatus(data.status());

                if (!data.password().isEmpty()) {
                    member.setPassword(
                        passwordEncoder.encode(data.password())
                    );
                }

                updated++;
            }

            saveList.add(member);
        }

        // 9. 寫入資料庫
        memberRepository.saveAllAndFlush(saveList);

        // 10. 回傳結果
        return new ExcelImportResponse(
            true,
            total,
            inserted,
            updated,
            0,
            "Excel匯入成功",
            List.of()
        );
    }

    // 取得一般欄位
    private String cellValue(
            Row row, int index, DataFormatter formatter) {

        return rawCellValue(row, index, formatter).trim();
    }

    // 取得原始欄位，密碼不trim
    private String rawCellValue(
            Row row, int index, DataFormatter formatter) {

        if (row == null) return "";

        Cell cell = row.getCell(index);

        if (cell == null) return "";

        if (cell.getCellType() == CellType.FORMULA) {
            return "";
        }

        return formatter.formatCellValue(cell);
    }

    private boolean isEmptyRow(
            Row row, DataFormatter formatter) {

        for (int i = 0; i < HEADERS.length; i++) {
            if (!rawCellValue(row, i, formatter).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private ExcelImportResponse failure(
            String message,
            int total,
            List<ExcelImportError> errors) {

        return new ExcelImportResponse(
            false,
            total,
            0,
            0,
            errors.size(),
            message,
            errors
        );
    }
}