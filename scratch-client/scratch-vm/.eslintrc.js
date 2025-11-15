module.exports = {
    env: {
        browser: true,
        es2021: true,
        node: true,
        jest: true
    },
    extends: [
        'eslint:recommended'
    ],
    parserOptions: {
        ecmaVersion: 2021,
        sourceType: 'module'
    },
    rules: {
        // Code Style - Following CONTRIBUTING.md guidelines
        'camelcase': ['error', {
            properties: 'always',
            ignoreDestructuring: false,
            allow: ['^_'] // Allow leading underscore for private methods
        }],
        'prefer-const': 'error',
        'no-var': 'error',
        'prefer-arrow-callback': 'error',
        'prefer-template': 'error',
        'template-curly-spacing': ['error', 'never'],

        // Best Practices
        'eqeqeq': ['error', 'always'],
        'no-console': 'off', // Allow console for educational debugging
        'no-unused-vars': ['warn', {
            argsIgnorePattern: '^_',
            varsIgnorePattern: '^_'
        }],
        'no-duplicate-imports': 'error',

        // Code Quality
        'complexity': ['warn', 15],
        'max-depth': ['warn', 4],
        'max-lines-per-function': ['warn', {
            max: 150,
            skipBlankLines: true,
            skipComments: true
        }],
        'max-params': ['warn', 5],

        // ES6+ Features
        'arrow-spacing': 'error',
        'arrow-parens': ['error', 'as-needed'],
        'object-shorthand': 'error',
        'prefer-destructuring': ['error', {
            array: false,
            object: true
        }, {
            enforceForRenamedProperties: false
        }],

        // Spacing and Formatting
        'indent': ['error', 4, {
            SwitchCase: 1
        }],
        'quotes': ['error', 'single', {
            avoidEscape: true,
            allowTemplateLiterals: true
        }],
        'semi': ['error', 'always'],
        'comma-dangle': ['error', 'never'],
        'space-before-function-paren': ['error', {
            anonymous: 'always',
            named: 'never',
            asyncArrow: 'always'
        }],
        'keyword-spacing': 'error',
        'space-infix-ops': 'error',
        'space-before-blocks': 'error',
        'object-curly-spacing': ['error', 'never'],
        'array-bracket-spacing': ['error', 'never'],
        'brace-style': ['error', '1tbs'],

        // Error Prevention
        'no-throw-literal': 'error',
        'no-return-await': 'error',
        'require-await': 'warn',
        'no-floating-decimal': 'error',
        'no-implicit-coercion': 'error'
    },
    overrides: [
        {
            files: ['**/__tests__/**/*.js', '**/*.test.js'],
            env: {
                jest: true
            },
            rules: {
                'max-lines-per-function': 'off' // Allow longer test functions
            }
        }
    ]
};
