//
// Created by Melanie Prettyman on 2/19/24.
//

#include "RC4.h"

//RC4 constructor
RC4::RC4(const std::string keyString) : S(256), i(0), j(0) {
    std::vector<uint8_t> key = std::vector<uint8_t> (keyString.begin(), keyString.end());

    //making state
    for (int i = 0; i < 256; i++) {
        S[i] = i;
    }
    // Key-Scheduling Algorithm (KSA)
    int j = 0;
    for (int i = 0; i < 256; i++) {
        j = (j + S[i] + key[i % key.size()]) % 256;
        swap(S[i], S[j]);
    }
}

//Generate Keystream by sequentially mix the values in S further based on its current state
// and output bytes of the keystream.
uint8_t RC4::getNextByte() {
    //Pseudo-random generation algorithm (PRGA)
    i = (i + 1) % 256;
    j = (j + S[i]) % 256;
    swap(S[i], S[j]);
    return static_cast<uint8_t>(S[(S[i] + S[j]) % 256]);
}

void RC4::swap(int &a, int &b) {
    int temp = a;
    a = b;
    b = temp;
}


