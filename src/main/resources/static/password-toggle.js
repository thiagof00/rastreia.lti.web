document.querySelectorAll('[data-password-toggle]').forEach(button => {
    const input = document.getElementById(button.getAttribute('aria-controls'));
    const icon = button.querySelector('i');

    button.addEventListener('click', () => {
        const mostrar = input.type === 'password';
        input.type = mostrar ? 'text' : 'password';
        button.setAttribute('aria-label', mostrar ? 'Ocultar senha' : 'Mostrar senha');
        icon.classList.toggle('ti-eye', !mostrar);
        icon.classList.toggle('ti-eye-off', mostrar);
    });
});
