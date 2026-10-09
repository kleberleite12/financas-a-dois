if ("serviceWorker" in navigator) {

    window.addEventListener("load", () => {

        navigator.serviceWorker
            .register("/service-worker.js")
            .then(registration => {

                console.log(
                    "Finanças a Dois PWA ativa.",
                    registration.scope
                );

            })
            .catch(error => {

                console.error(
                    "Erro ao registrar PWA:",
                    error
                );

            });

    });

}