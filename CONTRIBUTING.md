# Contributing to MinecraftEdu Scratch Controller

Thank you for your interest in contributing to MinecraftEdu Scratch Controller! 🎉

This document provides guidelines for contributing to this project. We welcome contributions from everyone, whether you're fixing a bug, adding a feature, or improving documentation.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Pull Request Process](#pull-request-process)
- [Coding Guidelines](#coding-guidelines)
- [Testing Guidelines](#testing-guidelines)
- [Commit Message Guidelines](#commit-message-guidelines)

## 🤝 Code of Conduct

This project is intended for educational purposes, primarily for children learning programming. Please be respectful, patient, and constructive in all interactions.

## 🚀 How Can I Contribute?

### 🐛 Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates. When creating a bug report, include:

- Clear description of the issue
- Steps to reproduce
- Expected vs actual behavior
- Environment details (Minecraft version, OS, browser, etc.)
- Relevant logs and screenshots

Use the [Bug Report template](.github/ISSUE_TEMPLATE/bug_report.md).

### 💡 Suggesting Enhancements

Enhancement suggestions are welcome! When suggesting a feature:

- Explain the educational value
- Describe the use case clearly
- Consider how it benefits students learning programming
- Provide examples or mockups if possible

Use the [Feature Request template](.github/ISSUE_TEMPLATE/feature_request.md).

### 📖 Improving Documentation

Documentation improvements are always appreciated:

- Fix typos and grammatical errors
- Clarify confusing sections
- Add missing information
- Translate documentation (especially for non-Japanese speakers)
- Add examples and tutorials

### 🔧 Code Contributions

We welcome code contributions! Areas where you can help:

- Bug fixes
- New block types
- New Minecraft commands
- Performance improvements
- Test coverage
- Code refactoring
- UI/UX improvements

## 💻 Development Setup

### Prerequisites

- **Node.js** 16.x or higher
- **npm** 8.x or higher
- **Java** 17 or higher
- **Minecraft** 1.20.1
- **Minecraft Forge** 47.2.0 or higher
- **Git**

### Setup Steps

1. **Fork and Clone**
   ```bash
   git clone https://github.com/YOUR_USERNAME/Scratch.git
   cd Scratch
   ```

2. **Install Dependencies**
   ```bash
   cd scratch-client/scratch-vm
   npm install
   ```

3. **Run Tests**
   ```bash
   npm test
   ```

4. **Run Tests with Coverage**
   ```bash
   npm run test:coverage
   ```

5. **Build**
   ```bash
   npm run build
   ```

6. **Development Mode**
   ```bash
   npm run dev
   ```

### Project Structure

```
minecraft-laughtare-project/
├── .github/                    # GitHub configuration
│   ├── workflows/              # CI/CD workflows
│   └── ISSUE_TEMPLATE/         # Issue templates
├── minecraft-mod/              # Minecraft Forge MOD
│   └── src/                    # MOD source code
├── scratch-client/             # Scratch extension
│   └── scratch-vm/             # Scratch VM extension
│       ├── src/                # Source code
│       │   └── extensions/
│       │       └── scratch3_minecraft/
│       │           └── index.js  # Main extension file
│       ├── __tests__/          # Test files
│       ├── jest.config.js      # Jest configuration
│       └── package.json        # Dependencies
├── LICENSE                     # MIT License
├── CHANGELOG.md                # Version history
├── CONTRIBUTING.md             # This file
└── README.md                   # Project documentation
```

## 🔄 Pull Request Process

1. **Create a Branch**
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/bug-description
   ```

2. **Make Changes**
   - Write clean, readable code
   - Follow the coding guidelines
   - Add tests for new functionality
   - Update documentation as needed

3. **Test Your Changes**
   ```bash
   npm test
   npm run test:coverage
   ```

4. **Commit Your Changes**
   ```bash
   git add .
   git commit -m "feat: add new block type for xyz"
   ```
   Follow the [Commit Message Guidelines](#commit-message-guidelines).

5. **Push to Your Fork**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Create Pull Request**
   - Use the [Pull Request template](.github/PULL_REQUEST_TEMPLATE.md)
   - Provide clear description of changes
   - Link related issues
   - Ensure all CI checks pass

7. **Code Review**
   - Address review comments
   - Update PR as needed
   - Be patient and respectful

## 📝 Coding Guidelines

### JavaScript Style

- Use **ES6+** syntax
- Use **camelCase** for variables and functions
- Use **PascalCase** for classes
- Use **UPPER_CASE** for constants
- Prefer `const` over `let`, avoid `var`
- Use arrow functions for callbacks
- Use template literals for string interpolation

### Code Quality

- **DRY Principle**: Don't repeat yourself
- **Single Responsibility**: Functions should do one thing well
- **Meaningful Names**: Use descriptive variable and function names
- **Comments**: Add comments for complex logic
- **Error Handling**: Handle errors gracefully with user-friendly messages

### Example

```javascript
// ✅ Good
const _validateCoordinate = (coord, name, min, max) => {
    const value = Number(coord);

    if (isNaN(value)) {
        console.error(`${name}が数値ではありません: ${coord}`);
        return 0;
    }

    if (value < min || value > max) {
        console.warn(`${name}が範囲外です（${min}〜${max}）: ${value}`);
        return Math.max(min, Math.min(max, value));
    }

    return value;
};

// ❌ Bad
function v(c, n, mi, ma) {
    var val = Number(c);
    if (isNaN(val)) return 0;
    if (val < mi || val > ma) return mi;
    return val;
}
```

### Child-Friendly Error Messages

Since this is an educational tool for children:

- ✅ "置こうとしているブロックが多すぎます！もう少し小さい範囲にしてみてください。"
- ❌ "範囲が大きすぎます（最大2,000,000ブロック）"

## 🧪 Testing Guidelines

### Writing Tests

- Add tests for all new functionality
- Test edge cases and error conditions
- Use descriptive test names
- Follow the AAA pattern: Arrange, Act, Assert

### Test Structure

```javascript
describe('FeatureName', () => {
    let instance;

    beforeEach(() => {
        instance = new Scratch3MinecraftBlocks(mockRuntime);
    });

    describe('methodName', () => {
        test('should do something specific', () => {
            // Arrange
            const input = 10;

            // Act
            const result = instance.methodName(input);

            // Assert
            expect(result).toBe(expectedValue);
        });
    });
});
```

### Running Tests

```bash
# Run all tests
npm test

# Run with coverage
npm run test:coverage

# Run in watch mode
npm run test:watch
```

### Coverage Goals

- Maintain at least **15%** overall coverage (current baseline)
- Aim for **80%+** coverage for critical helper methods
- Test all public API methods

## 📨 Commit Message Guidelines

We follow [Conventional Commits](https://www.conventionalcommits.org/) specification.

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, no logic change)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `perf`: Performance improvements

### Examples

```bash
# Feature
git commit -m "feat: add new teleport command with rotation"

# Bug fix
git commit -m "fix: correct Y-coordinate transformation for negative values"

# Documentation
git commit -m "docs: update README with new block types"

# Test
git commit -m "test: add unit tests for coordinate validation"

# Refactor
git commit -m "refactor: extract duplicate code into helper method"
```

### Commit with Co-Author (for pair programming)

```bash
git commit -m "feat: add new feature

Co-Authored-By: Claude <noreply@anthropic.com>"
```

## 🎓 Educational Focus

Remember that this project is primarily for education:

- **Simplicity over complexity**: Prefer clear, simple code over clever tricks
- **Learning opportunities**: Consider how features teach programming concepts
- **Age-appropriate**: Design for children (elementary to middle school)
- **Documentation**: Explain *why*, not just *what*
- **Error messages**: Use encouraging, helpful language

## 📞 Getting Help

- 💬 **Discussions**: Ask questions in [GitHub Discussions](../../discussions)
- 🐛 **Issues**: Report bugs using [Issue templates](.github/ISSUE_TEMPLATE/)
- 📧 **Contact**: Open an issue for general inquiries

## 🙏 Thank You!

Your contributions help make programming education more accessible and fun for children around the world. We appreciate your time and effort!

## 📄 License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

**Happy Contributing! 🚀**
