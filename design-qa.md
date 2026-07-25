# Career UI redesign QA

- Source visual truth:
  - `C:\Users\35975\AppData\Local\Temp\codex-clipboard-782af612-1b61-4883-b2a4-766fe3e883b2.png`
  - `C:\Users\35975\AppData\Local\Temp\codex-clipboard-1e1f0cec-0a1b-4ed9-a0f1-ae22f80afa7f.png`
  - `C:\Users\35975\AppData\Local\Temp\codex-clipboard-aff2e533-d6ee-4628-bc5e-790e3a21fcb8.png`
- Implementation screenshots:
  - `C:\Users\35975\AppData\Local\Temp\ai-interviewer-qa\career-planning.png`
  - `C:\Users\35975\AppData\Local\Temp\ai-interviewer-qa\career-plan-history.png`
  - `C:\Users\35975\AppData\Local\Temp\ai-interviewer-qa\career-assessment-history.png`
  - `C:\Users\35975\AppData\Local\Temp\ai-interviewer-qa\career-plan-detail.png`
- Combined comparison:
  - `C:\Users\35975\AppData\Local\Temp\ai-interviewer-qa\career-ui-comparison.png`
- Viewport: 1400 × 820 CSS pixels, density 1.
- State: career planning empty state; history lists with one representative record; career plan full-page detail.

## Full-view comparison evidence

The original career history screens were unstructured striped lists with actions detached at the right edge. The revised screens use the locked product tokens and the same hierarchy as Resume Center / Skills Library: 28px page titles, 13px subtitles, white bordered cards, 9–10px radii, indigo primary actions, semantic result badges, and consistent 16–18px internal spacing. The planning screen retains its original workflow while improving section hierarchy and empty-state clarity.

The combined comparison image places each original capture on the left and its revised JavaFX snapshot on the right.

## Focused region comparison evidence

- History records: icon tile, title, description, timestamp, result badge and actions now form one scan line. Buttons no longer sit outside the list container.
- Planning form: header icon, section icon and result empty state follow the same indigo icon-tile language as other product modules.
- Plan detail: navigation, title, saved timestamp, three metadata cards and the document surface are part of the main content route rather than a separate 750 × 550 stage.
- Interview report scores: the Markdown score table is synchronized with the same `scoreEvidence.scored` state used by the left rail. Evidence-insufficient dimensions display `证据不足` on both sides.

## Required fidelity surfaces

- Fonts and typography: shared application family and locked hierarchy retained; titles, module labels, metadata and captions use existing tokens.
- Spacing and layout rhythm: 22–24px page padding, 18px section gaps, 14–16px record padding and consistent card radii.
- Colors and visual tokens: shared application background, surface, border, primary and semantic warning/result colors.
- Image quality and assets: no new raster assets were required; all UI pictograms use the existing Ikonli Material Design icon library.
- Copy and content: Chinese labels were normalized and helper copy added without changing business semantics.

## Comparison history

1. P1: history pages rendered as plain striped lists and planning detail opened in a small modal stage.
   - Fix: rebuilt both lists as responsive record cards and routed the plan detail through `ContentNavigator`.
   - Post-fix evidence: `career-plan-history.png`, `career-assessment-history.png`, `career-plan-detail.png`.
2. P2: the first revised history snapshot exposed a horizontal scrollbar.
   - Fix: collapsed the history list's horizontal scrollbar and retained full-width cells.
   - Post-fix evidence: the revised history snapshots in `career-ui-comparison.png`.
3. P1: report left rail could show `证据不足` while the right Markdown table retained an AI estimate.
   - Fix: synchronized every dimension row before rendering, copying and exporting the report.
   - Post-fix evidence: `InterviewReportControllerTest.alignsMarkdownScoreTableWithEvidenceBackedSidebarValues`.

## Residual test note

JavaFX headless snapshots do not paint WebView document text unless the WebView is attached to a shown native window. The full-page detail route, document container and controller context were verified through the JavaFX FXML load test; Markdown rendering itself remains the existing shared `MarkdownView` behavior.

## Final result

final result: passed
