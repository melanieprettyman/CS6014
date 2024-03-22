/* login.c for u1060134 */

#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>

int check_secret(const char *password, int pwLen) {
  if (pwLen == -1) {
    printf("problem reading password.txt\n");
    return 0;
  } else {
    const char *pw = "superSecretPassword";
    return (pwLen == strlen(pw)) && (memcmp(password, pw, pwLen) == 0);
  }
}


extern char** environ;
static char * sh = "/bin/sh";
void success(){
  char  * argv[2] = {sh, NULL};
  puts("successful login!\n");
  execve(sh, argv, environ);
}

void failure(){
  puts("wrong password\n");
}


int login(){
  char password[40];
  int fd = open("password.txt", O_RDONLY);
  printf("enter your password:\n");
  int pwLen = read(fd, password, 1000); // just read the whole file...
  close(fd);
  return check_secret(password, pwLen);
}


int main(){
  int res = login();
  if (res) {
    success();
  } else {
    failure();
  }

  puts("exiting in main\n");
  return 0;
}
