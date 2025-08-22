#!/bin/bash

# ==============================================================================
# Whisper Android Build Script
# ==============================================================================
# This script provides automated build commands for different build variants
# and environments. It includes validation, testing, and deployment options.

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

# Default values
BUILD_TYPE="debug"
FLAVOR=""
CLEAN=false
RUN_TESTS=false
INSTALL=false
VERBOSE=false

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
Whisper Android Build Script

Usage: $0 [OPTIONS]

OPTIONS:
    -t, --type TYPE         Build type: debug, release, staging, benchmark (default: debug)
    -f, --flavor FLAVOR     Product flavor: free, pro (optional)
    -c, --clean             Clean build (remove build directory)
    --test                  Run tests after build
    --install               Install APK after build (requires connected device)
    -v, --verbose           Verbose output
    -h, --help              Show this help message

EXAMPLES:
    $0                                          # Build debug version
    $0 -t release -f pro                        # Build pro release version
    $0 -t debug --clean --test --install        # Clean build, test, and install debug
    $0 -t staging -f free --test                # Build free staging with tests

BUILD VARIANTS:
    debug       - Debug build with debugging enabled
    release     - Production release build
    staging     - Staging build for testing
    benchmark   - Benchmark build for performance testing

PRODUCT FLAVORS:
    free        - Free version with limited features
    pro         - Pro version with all features

EOF
}

validate_environment() {
    print_header "Validating Build Environment"
    
    # Check if we're in the right directory
    if [[ ! -f "$PROJECT_ROOT/app/build.gradle.kts" ]]; then
        print_error "Not in a valid Android project directory"
        exit 1
    fi
    
    # Check for required tools
    if ! command -v ./gradlew &> /dev/null; then
        print_error "Gradle wrapper not found"
        exit 1
    fi
    
    # Check Android SDK
    if [[ -z "$ANDROID_HOME" ]]; then
        print_warning "ANDROID_HOME not set, using default Android Studio location"
    fi
    
    # Check NDK
    if [[ -z "$ANDROID_NDK_HOME" ]]; then
        print_warning "ANDROID_NDK_HOME not set"
    fi
    
    print_success "Environment validation completed"
}

clean_build() {
    if [[ "$CLEAN" == true ]]; then
        print_header "Cleaning Build Directory"
        cd "$PROJECT_ROOT"
        ./gradlew clean
        print_success "Build directory cleaned"
    fi
}

run_tests() {
    if [[ "$RUN_TESTS" == true ]]; then
        print_header "Running Tests"
        cd "$PROJECT_ROOT"
        
        # Run unit tests
        print_info "Running unit tests..."
        ./gradlew test${BUILD_TYPE^}UnitTest
        
        # Run lint checks
        print_info "Running lint checks..."
        ./gradlew lint${BUILD_TYPE^}
        
        print_success "All tests passed"
    fi
}

build_apk() {
    print_header "Building APK"
    cd "$PROJECT_ROOT"
    
    # Construct gradle task name
    local task="assemble"
    if [[ -n "$FLAVOR" ]]; then
        task="${task}${FLAVOR^}"
    fi
    task="${task}${BUILD_TYPE^}"
    
    print_info "Running gradle task: $task"
    
    # Build with appropriate verbosity
    if [[ "$VERBOSE" == true ]]; then
        ./gradlew "$task" --info
    else
        ./gradlew "$task"
    fi
    
    # Find and display APK location
    local apk_dir="$BUILD_DIR/outputs/apk"
    if [[ -n "$FLAVOR" ]]; then
        apk_dir="$apk_dir/$FLAVOR"
    fi
    apk_dir="$apk_dir/$BUILD_TYPE"
    
    if [[ -d "$apk_dir" ]]; then
        local apk_file=$(find "$apk_dir" -name "*.apk" | head -1)
        if [[ -n "$apk_file" ]]; then
            print_success "APK built successfully: $apk_file"
            
            # Display APK info
            local apk_size=$(du -h "$apk_file" | cut -f1)
            print_info "APK size: $apk_size"
        fi
    fi
}

install_apk() {
    if [[ "$INSTALL" == true ]]; then
        print_header "Installing APK"
        
        # Check for connected devices
        if ! adb devices | grep -q "device$"; then
            print_error "No Android device connected"
            exit 1
        fi
        
        cd "$PROJECT_ROOT"
        
        # Construct gradle task name
        local task="install"
        if [[ -n "$FLAVOR" ]]; then
            task="${task}${FLAVOR^}"
        fi
        task="${task}${BUILD_TYPE^}"
        
        ./gradlew "$task"
        print_success "APK installed successfully"
    fi
}

generate_build_info() {
    print_header "Build Information"
    
    local build_time=$(date '+%Y-%m-%d %H:%M:%S')
    local git_commit=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    local git_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
    
    echo "Build Type: $BUILD_TYPE"
    echo "Product Flavor: ${FLAVOR:-"none"}"
    echo "Build Time: $build_time"
    echo "Git Commit: $git_commit"
    echo "Git Branch: $git_branch"
    echo "Clean Build: $CLEAN"
    echo "Run Tests: $RUN_TESTS"
    echo "Install APK: $INSTALL"
}

# ==============================================================================
# Main Script
# ==============================================================================

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--type)
            BUILD_TYPE="$2"
            shift 2
            ;;
        -f|--flavor)
            FLAVOR="$2"
            shift 2
            ;;
        -c|--clean)
            CLEAN=true
            shift
            ;;
        --test)
            RUN_TESTS=true
            shift
            ;;
        --install)
            INSTALL=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
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

# Validate build type
case $BUILD_TYPE in
    debug|release|staging|benchmark)
        ;;
    *)
        print_error "Invalid build type: $BUILD_TYPE"
        print_info "Valid types: debug, release, staging, benchmark"
        exit 1
        ;;
esac

# Validate flavor if provided
if [[ -n "$FLAVOR" ]]; then
    case $FLAVOR in
        free|pro)
            ;;
        *)
            print_error "Invalid flavor: $FLAVOR"
            print_info "Valid flavors: free, pro"
            exit 1
            ;;
    esac
fi

# Main execution
main() {
    print_header "Whisper Android Build Script"
    
    generate_build_info
    validate_environment
    clean_build
    build_apk
    run_tests
    install_apk
    
    print_header "Build Completed Successfully"
}

# Run main function
main
