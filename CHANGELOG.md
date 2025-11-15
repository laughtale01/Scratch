# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- MIT License file
- CHANGELOG.md for version tracking
- Comprehensive unit tests (51 test cases)
  - Block property construction tests
  - Y-coordinate transformation tests
  - Input validation tests
  - Token generation tests
- Jest configuration with coverage reporting
- Input validation for all coordinate-accepting methods
- User-friendly error messages for children

### Changed
- Improved error messages to be more child-friendly
- Enhanced WebSocket connection errors with troubleshooting steps
- Refactored duplicate code using DRY principles
- Updated README.md with correct block count (564 blocks)
- Unified Y-coordinate explanation across documentation

### Fixed
- Removed hardcoded authentication token
- Fixed security vulnerabilities in authentication
- Corrected documentation inconsistencies

## [1.0.1] - 2025-11-15

### Added
- Phase 3b: 50 additional blocks (shulker boxes, beds, candles)
- Comprehensive input validation for coordinates
- Helper methods for coordinate validation
  - `_validateCoordinate()` - Generic validator with NaN check and clamping
  - `_validateXZCoordinate()` - X/Z coordinate validation (±30,000,000)
  - `_validateYCoordinate()` - Y coordinate validation (-64 to 319)
- Helper methods for code quality
  - `_toMinecraftY()` / `_toScratchY()` - Y-coordinate transformation
  - `_buildBlockTypeWithProperties()` - Block property construction
  - `_generateClientToken()` - Unique authentication token generation

### Changed
- Refactored code to eliminate 60+ lines of duplication
- Improved code maintainability with DRY principles
- Enhanced security by removing hardcoded authentication
- Block property building logic consolidated into single method
- Y-coordinate conversion centralized with Y_OFFSET constant

### Fixed
- Documentation errors (block count 18 → 564)
- Y-coordinate explanation inconsistencies
- Range limit documentation (200,000 → 2,000,000 blocks)
- Removed duplicate blocks from block list

## [1.0.0] - 2025-11-14

### Added
- Initial release of MinecraftEdu Scratch Controller
- Scratch 3.0 extension for Minecraft control
- Minecraft 1.20.1 Forge MOD integration
- WebSocket communication (port 14711)
- 564 block types support
  - Basic blocks (stone, dirt, grass, etc.)
  - Wood variants (12 wood types × 8 variations)
  - Slabs (34 types)
  - Stairs (32 types)
  - Walls, fences, and gates (57 types)
  - Wool and carpet (16 colors)
  - Concrete and concrete powder (16 colors)
  - Terracotta (16 colors)
  - Glass and glass panes (17 types)
  - And many more!
- Block placement features
  - Absolute coordinates
  - Relative coordinates
  - Range filling (up to 2,000,000 blocks)
  - Placement types (bottom, top, double)
  - Facing directions (north, south, east, west)
- Entity summoning (50+ entity types)
- Player teleportation
- Chat message sending
- Player position retrieval
- Time and weather control
- Area clearing functions
- Entity clearing functions
- GitHub Pages deployment
- Live demo at https://laughtale01.github.io/Scratch/

### Technical Features
- Y-coordinate transformation (Scratch Y=0 → Minecraft Y=-60)
- Block state properties support (type, half, facing)
- Session management with unique client IDs
- Request/response handling with promise-based API
- Error handling and logging

## [0.1.0] - 2025-11-12

### Added
- Project initialization
- Basic Scratch VM extension structure
- MOD development environment setup
- Initial WebSocket server implementation
- Basic block placement functionality
- README.md with installation instructions

---

## Version History Summary

- **[Unreleased]** - Quality improvements, testing infrastructure, CI/CD
- **[1.0.1]** - 2025-11-15 - Quality improvements and validation
- **[1.0.0]** - 2025-11-14 - Initial public release with 564 blocks
- **[0.1.0]** - 2025-11-12 - Project initialization

---

## Links

- [GitHub Repository](https://github.com/laughtale01/Scratch)
- [Live Demo](https://laughtale01.github.io/Scratch/)
- [Issue Tracker](https://github.com/laughtale01/Scratch/issues)

---

## Contributing

We welcome contributions! Please see [README.md](README.md) for guidelines on how to contribute to this project.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
