#!/bin/bash

# ==============================================================================
# Whisper Android Release Build Script
# ==============================================================================
# This script handles the complete release build process including:
# - Environment validation
# - Code quality checks
# - Security validation
# - Release build generation
# - APK/AAB signing and verification

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$PROJECT_ROOT/app/build"
RELEASE_DIR="$PROJECT_ROOT/release"

# Build configuration
FLAVOR="pro"  # Default to pro version for release
BUILD_AAB=true
BUILD_APK=true
RUN_TESTS=true
RUN_LINT=true
UPLOAD_SYMBOLS=false

# ==============================================================================
# Helper Functions
# ==============================================================================

print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

show_help() {
    cat << EOF
Whisper Android Release Build Script

Usage: $0 [OPTIONS]

OPTIONS:
    -f, --flavor FLAVOR     Product flavor: free, pro (default: pro)
    --no-aab                Skip Android App Bundle generation
    --no-apk                Skip APK generation
    --no-tests              Skip running tests
    --no-lint               Skip lint checks
    --upload-symbols        Upload debug symbols (requires configuration)
    -h, --help              Show this help message

EXAMPLES:
    $0                      # Build pro release with all checks
    $0 -f free              # Build free release version
    $0 --no-tests           # Build without running tests (not recommended)

EOF
}

validate_release_environment() {
    print_header "Validating Release Environment"
    
    # Check if we're in the right directory
    if [[ ! -f "$PROJECT_ROOT/app/build.gradle.kts" ]]; then
        print_error "Not in a valid Android project directory"
        exit 1
    fi
    
    # Check for signing configuration
    if [[ -z "$KEYSTORE_FILE" ]] || [[ -z "$KEYSTORE_PASSWORD" ]] || [[ -z "$KEY_ALIAS" ]] || [[ -z "$KEY_PASSWORD" ]]; then
        print_warning "Release signing configuration not found in environment variables"
        print_info "Checking local.properties..."
        
        if [[ -f "$PROJECT_ROOT/local.properties" ]]; then
            if ! grep -q "KEYSTORE_FILE" "$PROJECT_ROOT/local.properties"; then
                print_error "Signing configuration not found. Please configure signing in local.properties or environment variables"
                exit 1
            fi
        else
            print_error "No signing configuration found. Release build requires proper signing setup"
            exit 1
        fi
    fi
    
    # Check Git status
    if git diff-index --quiet HEAD --; then
        print_success "Working directory is clean"
    else
        print_warning "Working directory has uncommitted changes"
        read -p "Continue with uncommitted changes? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
    
    # Check for required tools
    if ! command -v ./gradlew &> /dev/null; then
        print_error "Gradle wrapper not found"
        exit 1
    fi
    
    print_success "Release environment validation completed"
}

run_quality_checks() {
    print_header "Running Code Quality Checks"
    cd "$PROJECT_ROOT"
    
    if [[ "$RUN_LINT" == true ]]; then
        print_info "Running lint checks..."
        ./gradlew lint${FLAVOR^}Release
        
        # Check for lint errors
        local lint_results="$BUILD_DIR/reports/lint-results-${FLAVOR}Release.xml"
        if [[ -f "$lint_results" ]]; then
            if grep -q 'severity="Error"' "$lint_results"; then
                print_error "Lint errors found. Please fix before release"
                exit 1
            fi
        fi
        
        print_success "Lint checks passed"
    fi
    
    if [[ "$RUN_TESTS" == true ]]; then
        print_info "Running unit tests..."
        ./gradlew test${FLAVOR^}ReleaseUnitTest
        
        print_info "Running instrumentation tests..."
        if adb devices | grep -q "device$"; then
            ./gradlew connected${FLAVOR^}ReleaseAndroidTest || print_warning "Instrumentation tests failed or no device connected"
        else
            print_warning "No device connected, skipping instrumentation tests"
        fi
        
        print_success "Tests completed"
    fi
}

build_release_aab() {
    if [[ "$BUILD_AAB" == true ]]; then
        print_header "Building Release AAB (Android App Bundle)"
        cd "$PROJECT_ROOT"
        
        local task="bundle${FLAVOR^}Release"
        print_info "Running gradle task: $task"
        
        ./gradlew "$task"
        
        # Find and verify AAB
        local aab_dir="$BUILD_DIR/outputs/bundle/${FLAVOR}Release"
        local aab_file=$(find "$aab_dir" -name "*.aab" | head -1)
        
        if [[ -n "$aab_file" ]]; then
            print_success "AAB built successfully: $aab_file"
            
            # Display AAB info
            local aab_size=$(du -h "$aab_file" | cut -f1)
            print_info "AAB size: $aab_size"
            
            # Copy to release directory
            mkdir -p "$RELEASE_DIR"
            cp "$aab_file" "$RELEASE_DIR/"
            print_success "AAB copied to release directory"
        else
            print_error "AAB file not found"
            exit 1
        fi
    fi
}

build_release_apk() {
    if [[ "$BUILD_APK" == true ]]; then
        print_header "Building Release APK"
        cd "$PROJECT_ROOT"
        
        local task="assemble${FLAVOR^}Release"
        print_info "Running gradle task: $task"
        
        ./gradlew "$task"
        
        # Find and verify APK
        local apk_dir="$BUILD_DIR/outputs/apk/${FLAVOR}/release"
        local apk_file=$(find "$apk_dir" -name "*.apk" | head -1)
        
        if [[ -n "$apk_file" ]]; then
            print_success "APK built successfully: $apk_file"
            
            # Display APK info
            local apk_size=$(du -h "$apk_file" | cut -f1)
            print_info "APK size: $apk_size"
            
            # Verify APK signature
            if command -v apksigner &> /dev/null; then
                print_info "Verifying APK signature..."
                apksigner verify "$apk_file"
                print_success "APK signature verified"
            fi
            
            # Copy to release directory
            mkdir -p "$RELEASE_DIR"
            cp "$apk_file" "$RELEASE_DIR/"
            print_success "APK copied to release directory"
        else
            print_error "APK file not found"
            exit 1
        fi
    fi
}

generate_release_notes() {
    print_header "Generating Release Information"
    
    local release_info="$RELEASE_DIR/release-info.txt"
    local build_time=$(date '+%Y-%m-%d %H:%M:%S')
    local git_commit=$(git rev-parse HEAD 2>/dev/null || echo "unknown")
    local git_tag=$(git describe --tags --exact-match 2>/dev/null || echo "no-tag")
    local version_name=$(grep "versionName" "$PROJECT_ROOT/app/build.gradle.kts" | head -1 | sed 's/.*"\(.*\)".*/\1/')
    local version_code=$(grep "versionCode" "$PROJECT_ROOT/app/build.gradle.kts" | head -1 | sed 's/.*= *\([0-9]*\).*/\1/')
    
    cat > "$release_info" << EOF
Whisper Android Release Information
==================================

Build Information:
- Version Name: $version_name
- Version Code: $version_code
- Product Flavor: $FLAVOR
- Build Time: $build_time
- Git Commit: $git_commit
- Git Tag: $git_tag

Build Configuration:
- AAB Generated: $BUILD_AAB
- APK Generated: $BUILD_APK
- Tests Run: $RUN_TESTS
- Lint Checks: $RUN_LINT

Files Generated:
EOF
    
    # List generated files
    if [[ -d "$RELEASE_DIR" ]]; then
        find "$RELEASE_DIR" -name "*.aab" -o -name "*.apk" | while read -r file; do
            local filename=$(basename "$file")
            local filesize=$(du -h "$file" | cut -f1)
            echo "- $filename ($filesize)" >> "$release_info"
        done
    fi
    
    print_success "Release information generated: $release_info"
}

upload_debug_symbols() {
    if [[ "$UPLOAD_SYMBOLS" == true ]]; then
        print_header "Uploading Debug Symbols"
        
        # This would integrate with crash reporting services
        # Example: Firebase Crashlytics, Bugsnag, etc.
        print_info "Debug symbol upload would be implemented here"
        print_warning "Debug symbol upload not configured"
    fi
}

cleanup_build_artifacts() {
    print_header "Cleaning Up Build Artifacts"
    
    # Remove intermediate build files but keep outputs
    cd "$PROJECT_ROOT"
    ./gradlew clean
    
    print_success "Build artifacts cleaned up"
}

# ==============================================================================
# Main Script
# ==============================================================================

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--flavor)
            FLAVOR="$2"
            shift 2
            ;;
        --no-aab)
            BUILD_AAB=false
            shift
            ;;
        --no-apk)
            BUILD_APK=false
            shift
            ;;
        --no-tests)
            RUN_TESTS=false
            shift
            ;;
        --no-lint)
            RUN_LINT=false
            shift
            ;;
        --upload-symbols)
            UPLOAD_SYMBOLS=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Validate flavor
case $FLAVOR in
    free|pro)
        ;;
    *)
        print_error "Invalid flavor: $FLAVOR"
        print_info "Valid flavors: free, pro"
        exit 1
        ;;
esac

# Main execution
main() {
    print_header "Whisper Android Release Build"
    
    validate_release_environment
    run_quality_checks
    build_release_aab
    build_release_apk
    generate_release_notes
    upload_debug_symbols
    
    print_header "Release Build Completed Successfully"
    print_success "Release artifacts available in: $RELEASE_DIR"
    
    # Display final summary
    if [[ -d "$RELEASE_DIR" ]]; then
        echo
        print_info "Generated files:"
        ls -la "$RELEASE_DIR"
    fi
}

# Run main function
main
