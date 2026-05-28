import { api } from "@/store/api/apiSlice";
import { useAppDispatch } from "@/store/hooks";
import type {
	AvailableAppointmentSlot,
	ClinicService,
	Doctor,
	Specialization,
} from "@/api/types";
import { useEffect, useMemo, useRef, useState } from "react";
import type { UseFormReturn } from "react-hook-form";
import type { BookingFormValues } from "./booking-form-schema";
import { slotServiceLabel } from "./slot-service-label";

export function useBookingFormData(form: UseFormReturn<BookingFormValues>) {
	const dispatch = useAppDispatch();

	const [specialties, setSpecialties] = useState<Specialization[]>([]);
	const [allServicesCatalog, setAllServicesCatalog] = useState<ClinicService[]>([]);
	const [doctorServices, setDoctorServices] = useState<ClinicService[]>([]);
	const [allDoctors, setAllDoctors] = useState<Doctor[]>([]);
	const [doctors, setDoctors] = useState<Doctor[]>([]);
	const [slots, setSlots] = useState<AvailableAppointmentSlot[]>([]);
	const [isLoadingSpecialties, setIsLoadingSpecialties] = useState(false);
	const [isLoadingDoctors, setIsLoadingDoctors] = useState(false);
	const [isLoadingSlots, setIsLoadingSlots] = useState(false);
	const [selectedDoctorForReviews, setSelectedDoctorForReviews] = useState<{
		id: number;
		name: string;
	} | null>(null);
	const [isReviewsDialogOpen, setIsReviewsDialogOpen] = useState(false);
	const [availableDates, setAvailableDates] = useState<string[]>([]);
	const [isLoadingDates, setIsLoadingDates] = useState(false);
	const [datePickerOpen, setDatePickerOpen] = useState(false);

	const availableDateSet = useMemo(
		() => new Set(availableDates),
		[availableDates]
	);
	const datePickerContainerRef = useRef<HTMLDivElement>(null);

	useEffect(() => {
		if (!datePickerOpen) return;
		const closeIfOutside = (e: PointerEvent) => {
			const root = datePickerContainerRef.current;
			if (root && !root.contains(e.target as Node)) {
				setDatePickerOpen(false);
			}
		};
		document.addEventListener("pointerdown", closeIfOutside, true);
		return () => document.removeEventListener("pointerdown", closeIfOutside, true);
	}, [datePickerOpen]);

	useEffect(() => {
		if (!datePickerOpen) return;
		const onKey = (e: KeyboardEvent) => {
			if (e.key === "Escape") setDatePickerOpen(false);
		};
		document.addEventListener("keydown", onKey);
		return () => document.removeEventListener("keydown", onKey);
	}, [datePickerOpen]);

	const step = form.watch("step");
	const searchMode = form.watch("searchMode");
	const selectedSpecialty = form.watch("specialty");
	const selectedService = form.watch("service");
	const selectedDoctor = form.watch("doctor");
	const selectedDate = form.watch("date");

	useEffect(() => {
		if (!selectedDoctor) setDatePickerOpen(false);
	}, [selectedDoctor]);

	useEffect(() => {
		void fetchSpecialties();
	}, []);

	useEffect(() => {
		if (searchMode !== "by_service") return;
		let cancelled = false;
		void (async () => {
			try {
				const list = await dispatch(api.endpoints.getServices.initiate(undefined)).unwrap();
				if (!cancelled) setAllServicesCatalog(list);
			} catch {
				if (!cancelled) setAllServicesCatalog([]);
			}
		})();
		return () => {
			cancelled = true;
		};
	}, [dispatch, searchMode]);

	useEffect(() => {
		if (searchMode !== "by_specialty") {
			return;
		}
		if (selectedSpecialty) {
			void fetchDoctors();
		} else {
			setDoctors([]);
		}
	}, [selectedSpecialty, searchMode]);

	useEffect(() => {
		if (searchMode !== "by_service" || !selectedService?.trim()) {
			if (searchMode === "by_service") {
				setDoctors([]);
			}
			return;
		}
		const serviceId = Number(selectedService);
		if (!Number.isFinite(serviceId)) {
			setDoctors([]);
			return;
		}
		let cancelled = false;
		const req = dispatch(api.endpoints.getDoctors.initiate({ serviceId }));
		setIsLoadingDoctors(true);
		req
			.unwrap()
			.then((list) => {
				if (!cancelled) setDoctors(list);
			})
			.catch(() => {
				if (!cancelled) setDoctors([]);
			})
			.finally(() => {
				if (!cancelled) setIsLoadingDoctors(false);
			});
		return () => {
			cancelled = true;
			req.abort();
		};
	}, [dispatch, searchMode, selectedService]);

	useEffect(() => {
		if (!selectedDoctor) {
			setAvailableDates([]);
			return;
		}
		let cancelled = false;
		void (async () => {
			setIsLoadingDates(true);
			try {
				const sm = form.getValues("searchMode");
				const svc = form.getValues("service");
				const doctorId = Number(selectedDoctor);
				const opts: { serviceId?: number; from?: string; to?: string } = {};
				if (sm === "by_service" && svc?.trim()) {
					opts.serviceId = Number(svc);
				} else if (sm === "by_specialty" && svc?.trim()) {
					opts.serviceId = Number(svc);
				}
				const today = new Date();
				opts.from = today.toISOString().slice(0, 10);
				const toD = new Date(today);
				toD.setDate(toD.getDate() + 90);
				opts.to = toD.toISOString().slice(0, 10);
				const dates = await dispatch(
					api.endpoints.getAvailableDates.initiate({ doctorId, ...opts })
				).unwrap();
				if (!cancelled) {
					setAvailableDates(dates);
					const cur = form.getValues("date");
					if (cur && !dates.includes(cur)) {
						form.setValue("date", "");
						form.setValue("time", "");
						form.setValue("slot_id", undefined);
					}
				}
			} catch {
				if (!cancelled) setAvailableDates([]);
			} finally {
				if (!cancelled) setIsLoadingDates(false);
			}
		})();
		return () => {
			cancelled = true;
		};
	}, [dispatch, form, selectedDoctor, selectedService, searchMode]);

	useEffect(() => {
		if (searchMode !== "by_specialty" || !selectedDoctor) {
			setDoctorServices([]);
			return;
		}
		let cancelled = false;
		void (async () => {
			try {
				const list = await dispatch(
					api.endpoints.getServices.initiate({
						doctorId: Number(selectedDoctor),
					})
				).unwrap();
				if (!cancelled) setDoctorServices(list);
			} catch {
				if (!cancelled) setDoctorServices([]);
			}
		})();
		return () => {
			cancelled = true;
		};
	}, [dispatch, searchMode, selectedDoctor]);

	useEffect(() => {
		if (selectedDoctor && selectedDate) {
			void fetchSlots();
			form.setValue("time", "");
			form.setValue("slot_id", undefined);
		} else {
			setSlots([]);
		}
	}, [selectedDoctor, selectedDate, selectedService, searchMode]);

	async function fetchSpecialties() {
		setIsLoadingSpecialties(true);
		try {
			const response = await dispatch(api.endpoints.getSpecializations.initiate()).unwrap();
			setSpecialties(response);
		} finally {
			setIsLoadingSpecialties(false);
		}
	}

	async function fetchDoctors() {
		if (form.getValues("searchMode") !== "by_specialty") {
			return;
		}
		if (!selectedSpecialty) {
			setDoctors([]);
			return;
		}
		setIsLoadingDoctors(true);
		try {
			let doctorsToFilter = allDoctors;
			if (allDoctors.length === 0) {
				const response = await dispatch(api.endpoints.getDoctors.initiate(undefined)).unwrap();
				if (form.getValues("searchMode") !== "by_specialty") {
					return;
				}
				setAllDoctors(response);
				doctorsToFilter = response;
			}
			const filteredDoctors = doctorsToFilter.filter((doctor) =>
				doctor.specializations.some(
					(spec) => spec.id.toString() === selectedSpecialty
				)
			);
			if (form.getValues("searchMode") !== "by_specialty") {
				return;
			}
			setDoctors(filteredDoctors);
		} finally {
			if (form.getValues("searchMode") === "by_specialty") {
				setIsLoadingDoctors(false);
			}
		}
	}

	async function fetchSlots() {
		setIsLoadingSlots(true);
		try {
			const sm = form.getValues("searchMode");
			const svc = form.getValues("service");
			let opts: { serviceId?: number } | undefined;
			if (sm === "by_service" && svc?.trim()) {
				opts = { serviceId: Number(svc) };
			} else if (sm === "by_specialty" && svc?.trim()) {
				opts = { serviceId: Number(svc) };
			}
			const response = await dispatch(
				api.endpoints.getAvailableSlots.initiate({
					doctorId: Number(selectedDoctor),
					date: selectedDate,
					serviceId: opts?.serviceId ?? undefined,
				})
			).unwrap();
			setSlots(response);
		} finally {
			setIsLoadingSlots(false);
		}
	}

	const handleNextStep = () => {
		if (step !== 1) return;
		const sm = form.getValues("searchMode");
		const sp = form.getValues("specialty");
		const svc = form.getValues("service");
		if (sm === "by_specialty" && !sp.trim()) return;
		if (sm === "by_service" && !svc.trim()) return;
		if (
			!form.getValues("doctor") ||
			!form.getValues("date") ||
			!form.getValues("time")
		) {
			return;
		}
		form.setValue("step", 2);
	};

	const handlePrevStep = () => {
		if (step === 2) {
			form.setValue("step", 1);
		}
	};

	return {
		specialties,
		allServicesCatalog,
		doctorServices,
		doctors,
		slots,
		isLoadingSpecialties,
		isLoadingDoctors,
		isLoadingSlots,
		selectedDoctorForReviews,
		setSelectedDoctorForReviews,
		isReviewsDialogOpen,
		setIsReviewsDialogOpen,
		availableDates,
		isLoadingDates,
		datePickerOpen,
		setDatePickerOpen,
		availableDateSet,
		datePickerContainerRef,
		step,
		searchMode,
		selectedSpecialty,
		selectedService,
		selectedDoctor,
		selectedDate,
		setSlots,
		setDoctors,
		setAvailableDates,
		handleNextStep,
		handlePrevStep,
		slotServiceLabel,
	};
}
