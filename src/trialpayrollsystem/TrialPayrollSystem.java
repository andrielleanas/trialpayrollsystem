package trialpayrollsystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;

public class TrialPayrollSystem {

    public static void main(String[] args) {

        String empFile = "MotorPH Data/MotorPH - Employee Details.csv";
        String attFile = "MotorPH Data/MotorPH - Attendance Record.csv";

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter Username: ");
        String username = sc.nextLine();

        System.out.print("Enter Password: ");
        String password = sc.nextLine();

        if (!(username.equals("employee") || username.equals("payroll_staff"))
                || !password.equals("12345")) {
            System.out.println("Incorrect username and/or password.");
            return;
        }

        if (username.equals("employee")) {

            System.out.println("\n1. Enter Employee Number");
            System.out.println("2. Exit");
            int option = sc.nextInt();
            sc.nextLine();

            if (option == 2) return;

            System.out.print("Enter Employee Number: ");
            String empInput = sc.nextLine();

            showEmployeeInfo(empInput, empFile);
        }

        else if (username.equals("payroll_staff")) {

            System.out.println("\n1. Process Payroll");
            System.out.println("2. Exit");
            int option = sc.nextInt();
            sc.nextLine();
            if (option == 2) return;

            System.out.println("\n1. One Employee");
            System.out.println("2. All Employees");
            System.out.println("3. Exit");
            int choice = sc.nextInt();
            sc.nextLine();
            if (choice == 3) return;

            if (choice == 1) {
                System.out.print("Enter Employee Number: ");
                String empInput = sc.nextLine();
                processEmployee(empInput, empFile, attFile);
            } else if (choice == 2) {
                processAllEmployees(empFile, attFile);
            }
        }
    }

    // ================= EMPLOYEE SIDE (INFO) =================
    static void showEmployeeInfo(String empNo, String empFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(empFile))) {

            String header = br.readLine(); // skip header
            if (header == null) {
                System.out.println("Employee file is empty.");
                return;
            }

            String line;
            boolean found = false;

            while ((line = br.readLine()) != null) {
                String[] data = parseCSVLine(line);
                if (data.length > 0 && safeGet(data, 0).equals(empNo)) {
                    found = true;
                    System.out.println("\nEmployee Number: " + safeGet(data, 0));
                    System.out.println("Employee Name: " + safeGet(data, 1) + ", " + safeGet(data, 2));
                    System.out.println("Birthday: " + safeGet(data, 3));
                    break;
                }
            }

            if (!found) System.out.println("Employee number does not exist.");

        } catch (Exception e) {
            System.out.println("Error reading employee file.");
        }
    }

    // ================= PAYROLL STAFF: ALL EMPLOYEES =================
    static void processAllEmployees(String empFile, String attFile) {

        try (BufferedReader br = new BufferedReader(new FileReader(empFile))) {

            String header = br.readLine();
            if (header == null) {
                System.out.println("Employee file is empty.");
                return;
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] data = parseCSVLine(line);
                String empNo = safeGet(data, 0);
                if (!empNo.isEmpty()) {
                    processEmployee(empNo, empFile, attFile);
                }
            }

        } catch (Exception e) {
            System.out.println("Error reading employee file.");
        }
    }

    // ================= PROCESS EMPLOYEE (JUNE TO DEC) =================
    static void processEmployee(String inputEmpNo, String empFile, String attFile) {

        String empNo = "";
        String firstName = "";
        String lastName = "";
        String birthday = "";
        double rate = 0;
        boolean found = false;

        int hourlyRateIndex = 18; // fallback if header lookup fails

        try (BufferedReader br = new BufferedReader(new FileReader(empFile))) {

            String header = br.readLine();
            if (header != null) {
                String[] head = parseCSVLine(header);
                int idx = indexOf(head, "Hourly Rate");
                if (idx != -1) hourlyRateIndex = idx;
            }

            String line;
            while ((line = br.readLine()) != null) {

                String[] data = parseCSVLine(line);

                if (data.length > 0 && safeGet(data, 0).equals(inputEmpNo)) {

                    empNo = safeGet(data, 0);
                    lastName = safeGet(data, 1);
                    firstName = safeGet(data, 2);
                    birthday = safeGet(data, 3);

                    String rateStr = safeGet(data, hourlyRateIndex);
                    rate = parseMoney(rateStr);

                    found = true;
                    break;
                }
            }

        } catch (Exception e) {
            System.out.println("Error reading employee file.");
            return;
        }

        if (!found) {
            System.out.println("Employee number does not exist.");
            return;
        }

        System.out.println("\n===================================");
        System.out.println("Employee #: " + empNo);
        System.out.println("Employee Name: " + lastName + ", " + firstName);
        System.out.println("Birthday: " + birthday);
        System.out.println("Hourly Rate: " + rate);

        // Month buckets: June(6) to December(12)
        double[] hours1 = new double[13]; // index by month
        double[] hours2 = new double[13];

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("H:mm");

        // Attendance columns based on your current layout:
        // data[3] = date, data[4] = time in, data[5] = time out
        try (BufferedReader br = new BufferedReader(new FileReader(attFile))) {

            br.readLine(); // header
            String line;

            while ((line = br.readLine()) != null) {

                String[] data = parseCSVLine(line);
                if (data.length == 0) continue;

                if (!safeGet(data, 0).equals(empNo)) continue;

                LocalDate date = parseDate(safeGet(data, 3));
                if (date == null) continue;

                int month = date.getMonthValue();
                if (month < 6 || month > 12) continue; // June to December only

                LocalTime in = parseTime(safeGet(data, 4), timeFmt);
                LocalTime out = parseTime(safeGet(data, 5), timeFmt);
                if (in == null || out == null) continue;

                double hrs = computeHours(in, out);

                if (date.getDayOfMonth() <= 15) hours1[month] += hrs;
                else hours2[month] += hrs;
            }

        } catch (Exception e) {
            System.out.println("Error reading attendance file.");
            return;
        }

        // Display June to December
        for (int month = 6; month <= 12; month++) {

            YearMonth ym = YearMonth.of(2026, month); // year only used to get last day of month
            int lastDay = ym.lengthOfMonth();

            double gross1 = hours1[month] * rate;
            double gross2 = hours2[month] * rate;

            // Requirement: add cutoff 1 + cutoff 2 first, THEN compute deductions
            double combinedGross = gross1 + gross2;

            double sss = computeSSS(combinedGross);
            double phil = computePhilHealth(combinedGross);
            double pagibig = computePagIBIG(combinedGross);
            double tax = computeWithholdingTax(combinedGross);

            double totalDed = sss + phil + pagibig + tax;

            // Keep same payslip style: first cutoff = gross, second cutoff = gross - deductions
            double net1 = gross1;
            double net2 = gross2 - totalDed;

            System.out.println("\n---------- " + monthName(month) + " ----------");

            System.out.println("Cutoff: " + monthName(month) + " 1-15");
            System.out.println("Hours: " + hours1[month]);
            System.out.println("Gross: " + gross1);
            System.out.println("Net: " + net1);

            System.out.println("\nCutoff: " + monthName(month) + " 16-" + lastDay);
            System.out.println("Hours: " + hours2[month]);
            System.out.println("Gross: " + gross2);

            System.out.println("SSS: " + sss);
            System.out.println("PhilHealth: " + phil);
            System.out.println("Pag-IBIG: " + pagibig);
            System.out.println("Tax: " + tax);

            System.out.println("Total Deductions: " + totalDed);
            System.out.println("Net: " + net2);

            System.out.println("\nMonth Totals:");
            System.out.println("Combined Gross: " + combinedGross);
            System.out.println("Combined Net: " + (net1 + net2));
        }
    }

    // ================= HOURS RULES (8AM-5PM only, no extra hours) =================
    // Rules implemented:
    // - Only count time within 08:00 to 17:00
    // - 10-minute grace: time in <= 08:10 counts as 08:00
    // - If logs in at 08:30 and out at 17:30 => counts 08:30 to 17:00
    // - Deduct 1 hour lunch if there is enough time worked
    // - Max 8 hours/day
    static double computeHours(LocalTime actualIn, LocalTime actualOut) {

        LocalTime start = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(17, 0);
        LocalTime graceEnd = LocalTime.of(8, 10);

        // Clamp to work window
        LocalTime in;
        if (actualIn.isBefore(start) || actualIn.equals(start)) in = start;
        else if (!actualIn.isAfter(graceEnd)) in = start; // <= 8:10 becomes 8:00
        else in = actualIn;

        LocalTime out = actualOut.isAfter(end) ? end : actualOut;

        // If invalid range, 0 hours
        if (out.isBefore(in) || out.equals(in)) return 0;

        long minutes = java.time.Duration.between(in, out).toMinutes();

        // Deduct lunch: 60 minutes only if there's more than 60 minutes worked
        if (minutes > 60) minutes -= 60;
        else minutes = 0;

        double hours = minutes / 60.0;

        // No extra hours
        if (hours > 8) hours = 8;

        return hours;
    }

    // ================= DATE/TIME PARSING =================
    static LocalDate parseDate(String s) {
        // expected like M/d/yyyy or MM/dd/yyyy
        try {
            if (s == null) return null;
            String[] p = s.trim().split("/");
            if (p.length < 3) return null;

            int month = Integer.parseInt(p[0].trim());
            int day = Integer.parseInt(p[1].trim());
            int year = Integer.parseInt(p[2].trim());

            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    static LocalTime parseTime(String s, DateTimeFormatter fmt) {
        try {
            if (s == null) return null;
            s = s.trim();
            if (s.isEmpty()) return null;
            return LocalTime.parse(s, fmt);
        } catch (Exception e) {
            return null;
        }
    }

    static String monthName(int m) {
        switch (m) {
            case 6: return "June";
            case 7: return "July";
            case 8: return "August";
            case 9: return "September";
            case 10: return "October";
            case 11: return "November";
            case 12: return "December";
            default: return "Month " + m;
        }
    }

    // ================= CSV PARSER =================
    static String[] parseCSVLine(String line) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                out.add(stripQuotes(cur.toString().trim()));
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(stripQuotes(cur.toString().trim()));
        return out.toArray(new String[0]);
    }

    static String stripQuotes(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    static String safeGet(String[] arr, int idx) {
        if (arr == null) return "";
        if (idx < 0 || idx >= arr.length) return "";
        return arr[idx].trim();
    }

    static int indexOf(String[] headers, String target) {
        if (headers == null) return -1;
        for (int i = 0; i < headers.length; i++) {
            if (headers[i] != null && headers[i].trim().equalsIgnoreCase(target)) {
                return i;
            }
        }
        return -1;
    }

    static double parseMoney(String s) {
        try {
            if (s == null) return 0;
            s = stripQuotes(s).replace(",", "").trim();
            if (s.isEmpty()) return 0;
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    // ================= DEDUCTIONS (NO ROUNDING) =================
    static double computeSSS(double gross) {
        if (gross < 3250) return 135;
        else if (gross >= 24750) return 1125;
        else return 135 + ((int) ((gross - 3250) / 500) + 1) * 22.5;
    }

    static double computePhilHealth(double gross) {
        if (gross <= 10000) return 150;
        else if (gross < 60000) return gross * 0.015;
        else return 900;
    }

    static double computePagIBIG(double gross) {
        if (gross <= 1500) return gross * 0.01;
        else return gross * 0.02;
    }

    static double computeWithholdingTax(double gross) {
        if (gross <= 20832) return 0;
        else if (gross <= 33332) return (gross - 20833) * 0.20;
        else if (gross <= 66666) return 2500 + (gross - 33333) * 0.25;
        else if (gross <= 166666) return 10833 + (gross - 66667) * 0.30;
        else if (gross <= 666666) return 40833.33 + (gross - 166667) * 0.32;
        else return 200833.33 + (gross - 666667) * 0.35;
    }
}
