import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ProfanityService {

  private normalize(text: string): string {
    return text
      .toLowerCase()
      .replace(/ğ/g, 'g')
      .replace(/ı/g, 'i')
      .replace(/ş/g, 's')
      .replace(/ç/g, 'c')
      .replace(/ü/g, 'u')
      .replace(/ö/g, 'o');
  }

  // Words matched as substrings (unambiguously inappropriate)
  private readonly substringWords: string[] = [
    // English
    'fuck', 'bitch', 'cunt', 'pussy', 'bastard', 'asshole', 'motherfucker', 'faggot',
    // Turkish (normalized)
    'sik', 'orospu', 'yarrak', 'ibne', 'pezevenk', 'kahpe', 'kaltak', 'surtuk',
    'serefsiz', 'haysiyetsiz', 'amk', 'amina', 'amcik', 'orospu', 'yarak',
    'picligi', 'gotunu', 'sikeyim', 'sikiyor', 'siktir',
  ];

  // Words matched as whole words only (to avoid false positives)
  private readonly wholeWords: string[] = [
    'shit', 'ass', 'dick', 'cock', 'whore', 'slut',
    'am', 'pic', 'oc', 'got','bok',
  ];

  contains(text: string): boolean {
    const normalized = this.normalize(text);
    if (this.substringWords.some(word => normalized.includes(word))) {
      return true;
    }
    return this.wholeWords.some(word =>
      new RegExp(`(?<![a-z0-9])${word}(?![a-z0-9])`).test(normalized)
    );
  }
}
