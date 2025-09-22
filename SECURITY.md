# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.2.x   | :white_check_mark: |
| 1.1.x   | :white_check_mark: |
| < 1.1   | :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability in Echo Music, please report it responsibly:

1. **Do NOT** create a public GitHub issue
2. Email us at: [security@echo-music.com](mailto:security@echo-music.com)
3. Include the following information:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

## Security Best Practices

### For Developers
- Never commit API keys or sensitive configuration files
- Use template files for configuration
- Keep dependencies updated
- Follow secure coding practices

### For Users
- Only download from trusted sources
- Keep the app updated
- Report suspicious behavior immediately

## Configuration Security

### Firebase Configuration
- The `google-services.json` files contain sensitive information
- These files are excluded from the repository
- Use the provided template files for setup
- Never share your actual configuration files

### API Keys
- All API keys are managed through Firebase
- No hardcoded keys in the source code
- Keys are environment-specific

## Data Privacy

Echo Music is designed with privacy in mind:
- No personal data collection
- Local storage for user preferences
- Secure communication with external services
- No tracking or analytics

## License

This project is licensed under GPL-3.0, which ensures:
- Source code transparency
- Community security review
- No hidden backdoors
- Open development process

---

**Note**: This is a fork of SimpMusic with security improvements and additional features.
