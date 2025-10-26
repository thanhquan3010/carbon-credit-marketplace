// components/marketplace/SearchFilters.tsx
import React, { useState, useEffect } from 'react';
import {
    Paper,
    Box,
    Typography,
    TextField,
    Button,
    Accordion,
    AccordionSummary,
    AccordionDetails,
    Checkbox,
    FormControlLabel,
    FormGroup,
    Slider,
    Select,
    MenuItem,
    FormControl,
    InputLabel,
    Chip,
    Stack,
    InputAdornment,
    IconButton,
} from '@mui/material';
import {
    ExpandMore,
    Search,
    FilterList,
    Clear,
    LocationOn,
    DateRange,
    Category,
    VerifiedUser,
} from '@mui/icons-material';
import { useDispatch, useSelector } from 'react-redux';
import { RootState } from '../../store';
import { updateFilter, clearFilters } from '../../store/slices/marketplaceSlice';
import { marketplaceService } from '../../services/marketplaceService';

interface FilterOptions {
    projectTypes: string[];
    certificationBodies: string[];
    countries: string[];
    vintageYears: number[];
    priceRanges: Array<{ min: number; max: number; label: string }>;
}

export const SearchFilters: React.FC = () => {
    const dispatch = useDispatch();
    const filters = useSelector((state: RootState) => state.marketplace.filters);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterOptions, setFilterOptions] = useState<FilterOptions | null>(null);
    const [expandedSections, setExpandedSections] = useState<string[]>(['type', 'price']);

    // Local filter state
    const [selectedTypes, setSelectedTypes] = useState<string[]>([]);
    const [priceRange, setPriceRange] = useState<number[]>([0, 10000]);
    const [vintageRange, setVintageRange] = useState<number[]>([2020, 2024]);
    const [selectedCountry, setSelectedCountry] = useState('');
    const [verifiedOnly, setVerifiedOnly] = useState(false);

    useEffect(() => {
        fetchFilterOptions();
    }, []);

    useEffect(() => {
        // Initialize local state from Redux filters
        if (filters.projectType) setSelectedTypes(filters.projectType);
        if (filters.priceRange) setPriceRange([filters.priceRange.min, filters.priceRange.max]);
        if (filters.vintage) setVintageRange([filters.vintage.min, filters.vintage.max]);
        if (filters.location) setSelectedCountry(filters.location);
    }, [filters]);

    const fetchFilterOptions = async () => {
        try {
            const options = await marketplaceService.getFilterOptions();
            setFilterOptions(options);

            // Set initial price range based on available options
            if (options.priceRanges.length > 0) {
                const minPrice = Math.min(...options.priceRanges.map(r => r.min));
                const maxPrice = Math.max(...options.priceRanges.map(r => r.max));
                setPriceRange([minPrice, maxPrice]);
            }

            // Set initial vintage range
            if (options.vintageYears.length > 0) {
                const minYear = Math.min(...options.vintageYears);
                const maxYear = Math.max(...options.vintageYears);
                setVintageRange([minYear, maxYear]);
            }
        } catch (error) {
            console.error('Failed to fetch filter options:', error);
        }
    };

    const handleSearch = () => {
        const updatedFilters = {
            search: searchTerm,
            projectType: selectedTypes.length > 0 ? selectedTypes : undefined,
            priceRange: { min: priceRange[0], max: priceRange[1] },
            vintage: { min: vintageRange[0], max: vintageRange[1] },
            location: selectedCountry || undefined,
            verificationStatus: verifiedOnly ? ['VERIFIED'] : undefined,
        };

        dispatch(updateFilter(updatedFilters));
    };

    const handleClearFilters = () => {
        setSearchTerm('');
        setSelectedTypes([]);
        setPriceRange([0, 10000]);
        setVintageRange([2020, 2024]);
        setSelectedCountry('');
        setVerifiedOnly(false);
        dispatch(clearFilters());
    };

    const handleTypeToggle = (type: string) => {
        setSelectedTypes(prev =>
            prev.includes(type)
                ? prev.filter(t => t !== type)
                : [...prev, type]
        );
    };

    const handleAccordionChange = (panel: string) => {
        setExpandedSections(prev =>
            prev.includes(panel)
                ? prev.filter(p => p !== panel)
                : [...prev, panel]
        );
    };

    const activeFiltersCount =
        (selectedTypes.length > 0 ? 1 : 0) +
        (selectedCountry ? 1 : 0) +
        (verifiedOnly ? 1 : 0) +
        (searchTerm ? 1 : 0);

    return (
        <Paper elevation={2} sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                <Typography variant="h6" fontWeight="bold">
                    <FilterList sx={{ mr: 1, verticalAlign: 'middle' }} />
                    Filters
                </Typography>
                {activeFiltersCount > 0 && (
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Chip
                            label={`${activeFiltersCount} active`}
                            size="small"
                            color="primary"
                            variant="outlined"
                        />
                        <IconButton size="small" onClick={handleClearFilters}>
                            <Clear />
                        </IconButton>
                    </Box>
                )}
            </Box>

            {/* Search Bar */}
            <TextField
                fullWidth
                placeholder="Search carbon credits..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                InputProps={{
                    startAdornment: (
                        <InputAdornment position="start">
                            <Search />
                        </InputAdornment>
                    ),
                }}
                sx={{ mb: 2 }}
            />

            {/* Project Type */}
            <Accordion
                expanded={expandedSections.includes('type')}
                onChange={() => handleAccordionChange('type')}
            >
                <AccordionSummary expandIcon={<ExpandMore />}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Category fontSize="small" />
                        <Typography>Project Type</Typography>
                        {selectedTypes.length > 0 && (
                            <Chip label={selectedTypes.length} size="small" color="primary" />
                        )}
                    </Box>
                </AccordionSummary>
                <AccordionDetails>
                    <FormGroup>
                        {filterOptions?.projectTypes.map((type) => (
                            <FormControlLabel
                                key={type}
                                control={
                                    <Checkbox
                                        checked={selectedTypes.includes(type)}
                                        onChange={() => handleTypeToggle(type)}
                                        size="small"
                                    />
                                }
                                label={type}
                            />
                        ))}
                    </FormGroup>
                </AccordionDetails>
            </Accordion>

            {/* Price Range */}
            <Accordion
                expanded={expandedSections.includes('price')}
                onChange={() => handleAccordionChange('price')}
            >
                <AccordionSummary expandIcon={<ExpandMore />}>
                    <Typography>Price Range (per tCO2)</Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <Box sx={{ px: 2 }}>
                        <Slider
                            value={priceRange}
                            onChange={(e, newValue) => setPriceRange(newValue as number[])}
                            valueLabelDisplay="auto"
                            valueLabelFormat={(value) => `$${value}`}
                            min={0}
                            max={10000}
                            step={100}
                        />
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
                            <Typography variant="body2">${priceRange[0]}</Typography>
                            <Typography variant="body2">${priceRange[1]}</Typography>
                        </Box>
                    </Box>
                </AccordionDetails>
            </Accordion>

            {/* Vintage Year */}
            <Accordion
                expanded={expandedSections.includes('vintage')}
                onChange={() => handleAccordionChange('vintage')}
            >
                <AccordionSummary expandIcon={<ExpandMore />}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <DateRange fontSize="small" />
                        <Typography>Vintage Year</Typography>
                    </Box>
                </AccordionSummary>
                <AccordionDetails>
                    <Box sx={{ px: 2 }}>
                        <Slider
                            value={vintageRange}
                            onChange={(e, newValue) => setVintageRange(newValue as number[])}
                            valueLabelDisplay="auto"
                            min={2015}
                            max={2024}
                            step={1}
                            marks={[
                                { value: 2015, label: '2015' },
                                { value: 2020, label: '2020' },
                                { value: 2024, label: '2024' },
                            ]}
                        />
                    </Box>
                </AccordionDetails>
            </Accordion>

            {/* Location */}
            <Accordion
                expanded={expandedSections.includes('location')}
                onChange={() => handleAccordionChange('location')}
            >
                <AccordionSummary expandIcon={<ExpandMore />}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <LocationOn fontSize="small" />
                        <Typography>Location</Typography>
                        {selectedCountry && (
                            <Chip label={selectedCountry} size="small" color="primary" />
                        )}
                    </Box>
                </AccordionSummary>
                <AccordionDetails>
                    <FormControl fullWidth size="small">
                        <Select
                            value={selectedCountry}
                            onChange={(e) => setSelectedCountry(e.target.value)}
                            displayEmpty
                        >
                            <MenuItem value="">
                                <em>All Countries</em>
                            </MenuItem>
                            {filterOptions?.countries.map((country) => (
                                <MenuItem key={country} value={country}>
                                    {country}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                </AccordionDetails>
            </Accordion>

            {/* Verification Status */}
            <Accordion
                expanded={expandedSections.includes('verification')}
                onChange={() => handleAccordionChange('verification')}
            >
                <AccordionSummary expandIcon={<ExpandMore />}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <VerifiedUser fontSize="small" />
                        <Typography>Verification</Typography>
                        {verifiedOnly && (
                            <Chip label="Verified Only" size="small" color="success" />
                        )}
                    </Box>
                </AccordionSummary>
                <AccordionDetails>
                    <FormGroup>
                        <FormControlLabel
                            control={
                                <Checkbox
                                    checked={verifiedOnly}
                                    onChange={(e) => setVerifiedOnly(e.target.checked)}
                                />
                            }
                            label="Show only verified credits"
                        />
                    </FormGroup>
                </AccordionDetails>
            </Accordion>

            {/* Sort Options */}
            <FormControl fullWidth size="small" sx={{ mt: 2, mb: 2 }}>
                <InputLabel>Sort By</InputLabel>
                <Select
                    value={filters.sortBy || 'date_newest'}
                    onChange={(e) => dispatch(updateFilter({ sortBy: e.target.value }))}
                    label="Sort By"
                >
                    <MenuItem value="date_newest">Newest First</MenuItem>
                    <MenuItem value="date_oldest">Oldest First</MenuItem>
                    <MenuItem value="price_asc">Price: Low to High</MenuItem>
                    <MenuItem value="price_desc">Price: High to Low</MenuItem>
                    <MenuItem value="quantity_asc">Quantity: Low to High</MenuItem>
                    <MenuItem value="quantity_desc">Quantity: High to Low</MenuItem>
                </Select>
            </FormControl>

            {/* Apply Button */}
            <Button
                variant="contained"
                fullWidth
                onClick={handleSearch}
                startIcon={<Search />}
            >
                Apply Filters
            </Button>

            {/* Quick Filters */}
            <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                    Quick Filters
                </Typography>
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                    <Chip
                        label="Under $50"
                        size="small"
                        onClick={() => setPriceRange([0, 50])}
                        variant="outlined"
                        clickable
                    />
                    <Chip
                        label="Recent Vintage"
                        size="small"
                        onClick={() => setVintageRange([2022, 2024])}
                        variant="outlined"
                        clickable
                    />
                    <Chip
                        label="Renewable Energy"
                        size="small"
                        onClick={() => setSelectedTypes(['Renewable Energy'])}
                        variant="outlined"
                        clickable
                    />
                    <Chip
                        label="Verified Only"
                        size="small"
                        onClick={() => setVerifiedOnly(true)}
                        variant="outlined"
                        clickable
                    />
                </Stack>
            </Box>
        </Paper>
    );
};
