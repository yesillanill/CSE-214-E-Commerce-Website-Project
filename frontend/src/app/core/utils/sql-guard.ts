/**
 * SQL Injection detection utility for the frontend.
 * Detects raw SQL commands in user input before sending to the backend.
 */

const SQL_INJECTION_PATTERN = new RegExp(
  [
    // Dangerous DML/DDL
    '\\b(INSERT\\s+INTO|UPDATE\\s+\\w+\\s+SET|DELETE\\s+FROM)\\b',
    '\\b(DROP\\s+TABLE|DROP\\s+DATABASE|ALTER\\s+TABLE|TRUNCATE\\s+TABLE)\\b',
    '\\b(CREATE\\s+TABLE|CREATE\\s+DATABASE)\\b',
    // UNION injection
    '\\bUNION\\s+(ALL\\s+)?SELECT\\b',
    // Raw SELECT ... FROM
    '\\bSELECT\\s+.+\\s+FROM\\s+\\w+',
    // Schema probing
    '\\b(INFORMATION_SCHEMA|PG_CATALOG|PG_TABLES|SYSOBJECTS)\\b',
    // Statement terminators + keywords
    ';\\s*(DROP|ALTER|CREATE|TRUNCATE|INSERT|UPDATE|DELETE|EXEC|SELECT)\\b',
    // SQL comments
    '--\\s',
    '/\\*',
    // Time-based blind injection
    '\\b(PG_SLEEP|SLEEP|BENCHMARK|WAITFOR\\s+DELAY)\\s*\\(',
    // Hex payloads
    '0x[0-9a-fA-F]{8,}',
    // Tautology
    "'\\s*(OR|AND)\\s+\\d+\\s*=\\s*\\d+",
    // xp_ procedures
    '\\bxp_\\w+',
    // File operations
    '\\b(LOAD_FILE|INTO\\s+OUTFILE|INTO\\s+DUMPFILE)\\b',
  ].join('|'),
  'i'
);

/**
 * Check if a string contains SQL injection patterns.
 * @param input - The text to check.
 * @returns true if SQL injection is detected.
 */
export function containsSqlInjection(input: string): boolean {
  if (!input || input.trim().length === 0) return false;
  return SQL_INJECTION_PATTERN.test(input);
}

/**
 * SQL blocked message (Turkish + English bilingual).
 */
export const SQL_BLOCKED_MESSAGE =
  '🚫 Güvenlik uyarısı: Mesajınızda SQL komutları tespit edildi. SQL sorguları doğrudan yazılamaz. Lütfen sorunuzu doğal dilde ifade edin.\n\n' +
  '🚫 Security warning: SQL commands detected in your message. Raw SQL queries are not allowed. Please ask your question in natural language.';
