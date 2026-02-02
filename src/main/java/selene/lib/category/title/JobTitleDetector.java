package selene.lib.category.title;

import java.util.regex.Pattern;

public class JobTitleDetector {
    // BROADEST POSSIBLE JOB TITLE & OCCUPATION DETECTION
    private static final Pattern EXECUTIVE_PATTERN = Pattern.compile(
            "(?i)\\b(ceo|cto|cfo|cio|president|vp|director|head|chief|principal|founder|co-founder|owner|partner|gm|executive|chair|board|leadership|c-suite)\\b");

    private static final Pattern ENGINEERING_TECH_PATTERN = Pattern.compile(
            "(?i)\\b(engineer|software|systems|network|devops|data|ml|ai|developer|programmer|coder|dev|architect|sre|qa|test|backend|frontend|fullstack|mobile|it|tech|cloud|cyber|database|sysadmin|technician|mechanic)\\b");

    private static final Pattern MEDICAL_HEALTH_PATTERN = Pattern.compile(
            "(?i)\\b(doctor|physician|surgeon|md|dentist|nurse|therapist|psychologist|psychiatrist|pharmacist|health|medical|clinic|hospital|care|wellness|specialist|scientist|physio|promo|haematologist|radiographer|nutritionist|occupational|paramedic|technician|assistant|technologist)\\b");

    private static final Pattern CREATIVE_PATTERN = Pattern.compile(
            "(?i)\\b(designer|ux|ui|graphic|art|creative|editor|writer|content|media|producer|journalist|reporter|photographer|video|multimedia|illustrator|artist|composer|director|curator|copywriter|commissioning)\\b");

    private static final Pattern BUSINESS_SALES_PATTERN = Pattern.compile(
            "(?i)\\b(sales|account|rep|agent|business|marketing|growth|brand|advertising|digital|seo|buyer|merchant|retail|wholesale|trade|commerce|broker|consultant|advisor|analyst)\\b");

    private static final Pattern SUPPORT_ADMIN_PATTERN = Pattern.compile(
            "(?i)\\b(support|help|customer|service|admin|administrator|assistant|coordinator|operator|officer|receptionist|secretary|clerk|dispatcher|attendant)\\b");

    private static final Pattern FINANCE_PATTERN = Pattern.compile(
            "(?i)\\b(accountant|finance|banker|cashier|audit|tax|investor|fund|wealth|insurance|actuary|bookkeeper|controller|treasurer|trader|broker|teller)\\b");

    private static final Pattern HR_PATTERN = Pattern.compile(
            "(?i)\\b(hr|human|resource|recruiter|talent|personnel|training|coach|trainer|mentor|l&d|staffing|payroll)\\b");

    private static final Pattern OPERATIONS_PATTERN = Pattern.compile(
            "(?i)\\b(operations|ops|logistic|supply|warehouse|project|manager|coordinator|planner|procurement|inventory|facilities|maintenance|technician)\\b");

    private static final Pattern LEGAL_PATTERN = Pattern.compile(
            "(?i)\\b(lawyer|attorney|solicitor|judge|legal|paralegal|compliance|regulatory|contract|policy|risk|compliance|officer)\\b");

    private static final Pattern EDUCATION_PATTERN = Pattern.compile(
            "(?i)\\b(teacher|professor|lecturer|instructor|tutor|educator|principal|dean|coach|trainer|mentor|counselor|academic|scholar)\\b");

    private static final Pattern SCIENTIFIC_PATTERN = Pattern.compile(
            "(?i)\\b(scientist|researcher|biologist|chemist|physicist|geologist|analyst|lab|laboratory|biology|chemistry|physics|math|statistic|technician)\\b");

    // UNIVERSAL OCCUPATION CATCH-ALL PATTERNS
    private static final Pattern SENIORITY_PATTERN = Pattern.compile(
            "(?i)\\b(senior|sr|junior|jr|mid|mid-level|lead|principal|staff|distinguished|fellow|master|expert|specialist|associate)\\b");

    private static final Pattern MANAGEMENT_PATTERN = Pattern.compile(
            "(?i)\\b(manage|manager|director|supervisor|team lead|head|chief|boss|oversee|control|superintendent|foreman)\\b");

    private static final Pattern WORKER_PATTERN = Pattern.compile(
            "(?i)\\b(worker|operator|technician|mechanic|driver|pilot|officer|agent|rep|clerk|assistant|helper|aide|attendant)\\b");

    public static boolean isJobTitle(String string) {
        if (string == null || string.trim().isEmpty()) {
            return false;
        }

        String cleanString = string.trim();

        return EXECUTIVE_PATTERN.matcher(cleanString).find() ||
                ENGINEERING_TECH_PATTERN.matcher(cleanString).find() ||
                MEDICAL_HEALTH_PATTERN.matcher(cleanString).find() ||
                CREATIVE_PATTERN.matcher(cleanString).find() ||
                BUSINESS_SALES_PATTERN.matcher(cleanString).find() ||
                SUPPORT_ADMIN_PATTERN.matcher(cleanString).find() ||
                FINANCE_PATTERN.matcher(cleanString).find() ||
                HR_PATTERN.matcher(cleanString).find() ||
                OPERATIONS_PATTERN.matcher(cleanString).find() ||
                LEGAL_PATTERN.matcher(cleanString).find() ||
                EDUCATION_PATTERN.matcher(cleanString).find() ||
                SCIENTIFIC_PATTERN.matcher(cleanString).find() ||
                SENIORITY_PATTERN.matcher(cleanString).find() ||
                MANAGEMENT_PATTERN.matcher(cleanString).find() ||
                WORKER_PATTERN.matcher(cleanString).find();
    }
}
