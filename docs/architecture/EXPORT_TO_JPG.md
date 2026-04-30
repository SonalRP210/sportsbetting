# Export Mermaid Diagrams to PNG

From the project root, run:

```bash
npx -y @mermaid-js/mermaid-cli -i "docs/architecture/start-architecture.mmd" -o "docs/architecture/start-architecture.png" -w 2200 -H 1400 -b white
npx -y @mermaid-js/mermaid-cli -i "docs/architecture/phase1-architecture.mmd" -o "docs/architecture/phase1-architecture.png" -w 2200 -H 1400 -b white
npx -y @mermaid-js/mermaid-cli -i "docs/architecture/phase2-architecture.mmd" -o "docs/architecture/phase2-architecture.png" -w 2200 -H 1400 -b white
npx -y @mermaid-js/mermaid-cli -i "docs/architecture/phase3-architecture.mmd" -o "docs/architecture/phase3-architecture.png" -w 2400 -H 1500 -b white
```

Generated files:

- `docs/architecture/start-architecture.png`
- `docs/architecture/phase1-architecture.png`
- `docs/architecture/phase2-architecture.png`
- `docs/architecture/phase3-architecture.png`

Note: mermaid-cli supports `.md`, `.svg`, `.png`, and `.pdf` output formats, but not `.jpg`.

