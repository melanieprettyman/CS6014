# Define the compiler
CXX = g++

# Compiler flags
CXXFLAGS = -std=c++17 -Wall

# Build target executable:
TARGET = RC4

# Source files
SOURCES = main.cpp RC4.cpp

# Object files
OBJECTS = $(SOURCES:.cpp=.o)

# Header files
HEADERS = RC4.h

# Rule to link the object files into the executable
$(TARGET): $(OBJECTS)
	$(CXX) $(CXXFLAGS) -o $(TARGET) $(OBJECTS)

# Rule to compile the source files into object files
%.o: %.cpp $(HEADERS)
	$(CXX) $(CXXFLAGS) -c $< -o $@

# Phony target to clean the build
.PHONY: clean
clean:
	rm -f $(TARGET) $(OBJECTS)
