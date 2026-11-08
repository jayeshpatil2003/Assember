section .data
msg db "Hello", 10
count dd 5

section .text
global _start
_start:
    mov eax, count
    mov ebx, 0
    add eax, ebx
