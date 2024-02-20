// RC4.h
#ifndef RC4_H
#define RC4_H

#include <vector>
#include <cstdint>
#include <string>


class RC4 {
private:
    std::vector<int> S;
    int i, j;

    void swap(int &a, int &b);

public:
    RC4(const std::string keyString);
    uint8_t getNextByte();
};

#endif // RC4_H
